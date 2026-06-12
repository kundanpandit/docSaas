import { NavLink } from "react-router-dom";

const NavItem = ({ item }) => {
  const Icon = item.icon;

  return (
    <NavLink
      to={item.path}
      className={({ isActive }) =>
        `flex items-center gap-3 rounded-lg px-3 py-2 transition-all ${
          isActive
            ? "bg-primary text-primary-foreground"
            : "hover:bg-muted"
        }`
      }
    >
      <Icon size={18} />
      <span>{item.title}</span>
    </NavLink>
  );
};

export default NavItem;