#!/usr/bin/env bash
# One-time hardening + setup for a fresh Ubuntu 22.04/24.04 server (x86_64 or Arm, e.g. Oracle Cloud A1).
# Run as the default sudo user (ubuntu) from the repo's deploy/ folder:   sudo bash setup-server.sh
# Safe to re-run.
set -euo pipefail

[[ $EUID -eq 0 ]] || { echo "Run with sudo"; exit 1; }
APP_USER=${SUDO_USER:-ubuntu}
DEPLOY_DIR=$(cd "$(dirname "$0")" && pwd)
export DEBIAN_FRONTEND=noninteractive

echo "==> System updates + automatic security updates"
apt-get update -y
apt-get upgrade -y
apt-get install -y ca-certificates curl gnupg ipset iptables-persistent netfilter-persistent ipset-persistent \
  unattended-upgrades fail2ban openssl cron
dpkg-reconfigure -f noninteractive unattended-upgrades

echo "==> Time zone"
timedatectl set-timezone Asia/Kolkata || true

echo "==> Swap (4 GB) so LibreOffice/OCR spikes cannot OOM-kill the server"
if ! swapon --show | grep -q /swapfile; then
  fallocate -l 4G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
  grep -q '^/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

echo "==> SSH: keys only, no root login"
SSHD=/etc/ssh/sshd_config.d/99-docsaas.conf
cat > "$SSHD" <<'EOF'
PasswordAuthentication no
KbdInteractiveAuthentication no
PermitRootLogin no
MaxAuthTries 3
X11Forwarding no
EOF
systemctl reload ssh || systemctl reload sshd || true

echo "==> fail2ban for SSH"
cat > /etc/fail2ban/jail.d/sshd.local <<'EOF'
[sshd]
enabled = true
maxretry = 5
bantime = 1h
findtime = 10m
EOF
systemctl enable --now fail2ban

echo "==> Docker Engine + Compose plugin (official repository)"
if ! command -v docker >/dev/null; then
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc
  . /etc/os-release
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu ${VERSION_CODENAME} stable" \
    > /etc/apt/sources.list.d/docker.list
  apt-get update -y
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi
usermod -aG docker "$APP_USER"
systemctl enable --now docker
# Container logs are size-limited per service in the compose file too
mkdir -p /etc/docker
[[ -f /etc/docker/daemon.json ]] || echo '{"log-driver":"json-file","log-opts":{"max-size":"20m","max-file":"5"},"live-restore":true}' > /etc/docker/daemon.json
systemctl restart docker

echo "==> Firewall"
# Host ports: SSH stays open (key-only + fail2ban). Oracle images end INPUT with a REJECT rule,
# so 80/443 are inserted before it.
for port in 80 443; do
  if ! iptables -C INPUT -p tcp --dport "$port" -m conntrack --ctstate NEW -j ACCEPT 2>/dev/null; then
    REJECT_LINE=$(iptables -L INPUT --line-numbers -n | awk '$2=="REJECT"{print $1; exit}')
    if [[ -n "$REJECT_LINE" ]]; then
      iptables -I INPUT "$REJECT_LINE" -p tcp --dport "$port" -m conntrack --ctstate NEW -j ACCEPT
    else
      iptables -A INPUT -p tcp --dport "$port" -m conntrack --ctstate NEW -j ACCEPT
    fi
  fi
done
# Docker-published ports skip INPUT, so the Cloudflare-only rule goes in DOCKER-USER:
# new connections to 80/443 from anywhere except Cloudflare are dropped.
ipset create cloudflare4 hash:net -exist
ipset flush cloudflare4
for net in $(curl -fsS https://www.cloudflare.com/ips-v4); do ipset add cloudflare4 "$net" -exist; done
EXT_IF=$(ip route show default | awk '{print $5; exit}')
iptables -N DOCKER-USER 2>/dev/null || true
iptables -C DOCKER-USER -i "$EXT_IF" -p tcp -m multiport --dports 80,443 -m conntrack --ctstate NEW \
  -m set ! --match-set cloudflare4 src -j DROP 2>/dev/null \
  || iptables -I DOCKER-USER -i "$EXT_IF" -p tcp -m multiport --dports 80,443 -m conntrack --ctstate NEW \
       -m set ! --match-set cloudflare4 src -j DROP
netfilter-persistent save

echo "==> deploy/.env with fresh secrets"
ENV="$DEPLOY_DIR/.env"
if [[ ! -f "$ENV" ]]; then
  cp "$DEPLOY_DIR/.env.example" "$ENV"
  sed -i "s|^DB_PASSWORD=.*|DB_PASSWORD=$(openssl rand -hex 24)|" "$ENV"
  sed -i "s|^DB_ROOT_PASSWORD=.*|DB_ROOT_PASSWORD=$(openssl rand -hex 24)|" "$ENV"
  sed -i "s|^JWT_SECRET=.*|JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')|" "$ENV"
  sed -i "s|^JOBRUNR_DASHBOARD_PASSWORD=.*|JOBRUNR_DASHBOARD_PASSWORD=$(openssl rand -hex 16)|" "$ENV"
  echo "    created $ENV - now fill in GOOGLE_CLIENT_ID, AI_API_KEY and MAIL_* values"
fi
chown "$APP_USER": "$ENV" && chmod 600 "$ENV"
mkdir -p "$DEPLOY_DIR/certs" "$DEPLOY_DIR/backups"
chown -R "$APP_USER": "$DEPLOY_DIR/certs" "$DEPLOY_DIR/backups"
chmod 700 "$DEPLOY_DIR/certs" "$DEPLOY_DIR/backups"

echo "==> Nightly database backup at 02:30 (kept 14 days)"
CRON="30 2 * * * $APP_USER cd $DEPLOY_DIR && bash backup.sh >> $DEPLOY_DIR/backups/backup.log 2>&1"
echo "$CRON" > /etc/cron.d/docsaas-backup
chmod 644 /etc/cron.d/docsaas-backup

# Refresh the Cloudflare IP list weekly
cat > /etc/cron.weekly/cloudflare-ips <<'EOF'
#!/bin/sh
ips=$(curl -fsS https://www.cloudflare.com/ips-v4) || exit 0
ipset create cloudflare4-new hash:net -exist && ipset flush cloudflare4-new
for net in $ips; do ipset add cloudflare4-new "$net" -exist; done
ipset swap cloudflare4-new cloudflare4 && ipset destroy cloudflare4-new && netfilter-persistent save
EOF
chmod 755 /etc/cron.weekly/cloudflare-ips

echo
echo "Done. Log out and back in (docker group), then:"
echo "  1. put the Cloudflare Origin Certificate in $DEPLOY_DIR/certs/origin.pem and origin.key (chmod 600)"
echo "  2. fill in $ENV"
echo "  3. cd $DEPLOY_DIR && docker compose -f docker-compose.prod.yml --env-file .env up -d --build"
