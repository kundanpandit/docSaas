# Deploying DocSaaS to docsaas.in

```
visitor ──HTTPS──► Cloudflare (DNS, CDN, DDoS, WAF)
                     ├─ docsaas.in, www.docsaas.in ─► Cloudflare Pages (React build of docsaas-ui)
                     └─ api.docsaas.in ──HTTPS (Origin cert, Full strict)──► server :443
                                                                              Caddy ─► api:8080
                                                                              worker  (jobs)
                                                                              mysql   (private)
```

Security built in:

- The server accepts web traffic **only from Cloudflare** (iptables `DOCKER-USER` + Caddy `remote_ip` check), so the origin IP can't be attacked directly.
- MySQL, the API and the workers have **no public ports**. The JobRunr dashboard listens on the server's localhost only.
- The app uses its own MySQL user, and every secret is freshly generated on the server. Development passwords and the JWT secret are never reused.
- Real visitor IPs come from Cloudflare's `CF-Connecting-IP`, trusted only from Cloudflare's ranges, so guest credits and rate limits can't be spoofed.
- SSH accepts keys only. Root login is disabled, fail2ban and automatic security updates are on, and the database is backed up nightly with 14 days kept.
- Security headers are set on both the API and the site, including a strict CSP verified with zero violations.

---

## 1. Cloudflare (DNS + HTTPS)

1. Create a free account at dash.cloudflare.com, then **Add a site → `docsaas.in` → Free plan**.
2. At the registrar where you bought `docsaas.in`, replace the nameservers with the two Cloudflare gives you. It takes minutes to a few hours.
3. Go to **SSL/TLS → Overview** and set the mode to **Full (strict)**.
4. Under **SSL/TLS → Edge Certificates**, turn on **Always Use HTTPS** and set **Minimum TLS 1.2**.
5. Under **SSL/TLS → Origin Server → Create certificate**, use hostnames `docsaas.in, *.docsaas.in`, valid for 15 years.
   Keep the two text blocks: the certificate becomes `origin.pem` and the private key becomes `origin.key`. The key is shown only once.

## 2. Server (Oracle Cloud Always Free)

1. Create an account at signup.oraclecloud.com with home region **India West (Mumbai)** or **India South (Hyderabad)**.
2. Go to **Compute → Instances → Create**:
   - Image: **Ubuntu 24.04**.
   - Shape: **Ampere VM.Standard.A1.Flex**, 4 OCPU / 24 GB (2 / 12 is enough to start).
   - SSH key: paste your **public** key. On Windows, run `ssh-keygen -t ed25519` and use `C:\Users\<you>\.ssh\id_ed25519.pub`.
3. Go to **Networking → your VCN → Security List** and add ingress rules for **TCP 80 and 443 from 0.0.0.0/0**. The server firewall then narrows this to Cloudflare only.
4. Note the instance's **public IP**.

If Oracle says "out of capacity", try again later or in another availability domain. Any Ubuntu server with 4 GB+ RAM also works (Hetzner, DigitalOcean).

## 3. DNS records (Cloudflare → DNS)

| Type | Name | Content | Proxy |
|---|---|---|---|
| A | `api` | server public IP | **Proxied** (orange cloud) |

`docsaas.in` and `www` are added automatically when you attach them to Pages in step 6.

## 4. Put the code on the server and harden it

```bash
ssh ubuntu@<server-ip>
git clone git@github.com:kundanpandit/docSaas.git docsaas   # or scp the folder
cd docsaas/deploy
sudo bash setup-server.sh        # updates, swap, Docker, firewall, SSH hardening, backups, fresh secrets
exit                             # log out and back in so the docker group applies
```

## 5. Certificates, settings, start

```bash
cd ~/docsaas/deploy
nano certs/origin.pem            # paste the Cloudflare Origin certificate
nano certs/origin.key            # paste its private key
chmod 600 certs/*
nano .env                        # fill GOOGLE_CLIENT_ID, AI_API_KEY, MAIL_* (secrets are already generated)
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
docker compose -f docker-compose.prod.yml logs -f api   # wait for "Started DocsaasApiApplication"
curl https://api.docsaas.in/api/v1/health                # from your laptop
```

## 6. Frontend (Cloudflare Pages)

1. Go to **Workers & Pages → Create → Pages → Connect to Git** and pick the GitHub repo.
2. Use these settings: root directory **`docsaas-ui`**, build command **`npm ci && npm run build`**, output **`dist`**, environment variable **`NODE_VERSION=22`**.
   `docsaas-ui/.env.production` already points at `https://api.docsaas.in/api`.
3. Under **Custom domains**, add **`docsaas.in`** and **`www.docsaas.in`**.

## 7. Integrations

- **Google sign-in:** in Google Cloud Console, open **APIs & Services → Credentials → the OAuth client** and add **Authorized JavaScript origins** `https://docsaas.in` and `https://www.docsaas.in`.
- **Email (Brevo):**
  1. Add the domain `docsaas.in` in Brevo and copy the DNS records it shows (DKIM, SPF TXT, DMARC) into Cloudflare DNS as **DNS only**.
  2. Put the Brevo SMTP login and key in `.env`.
  3. Run `docker compose ... up -d` again.
- **Gemini:** set the new (regenerated) key as `AI_API_KEY` in `.env`.

## Everyday operations

```bash
cd ~/docsaas/deploy
C="docker compose -f docker-compose.prod.yml --env-file .env"
git pull && $C up -d --build                      # deploy a new version
$C logs -f --tail=200 api worker                  # logs
$C up -d --scale worker=3                         # more processing capacity
bash backup.sh                                    # backup now (nightly runs automatically at 02:30)
ssh -L 8000:localhost:8000 ubuntu@<server-ip>     # then open http://localhost:8000 for the job dashboard
```

Restore a backup:

```bash
gunzip -c backups/docsaas-YYYY-MM-DD.sql.gz | $C exec -T mysql sh -c 'mysql -u root -p"$MYSQL_ROOT_PASSWORD" docsaas_db'
```
