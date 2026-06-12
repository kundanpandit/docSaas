import { useSelector } from "react-redux"
import { useDispatch } from "react-redux";
import { logout } from "@/features/auth/slice/authSlice";
import { storage } from "@/lib/storage";
import { Button } from "@/components/ui/button";
import { useNavigate } from "react-router-dom";

const DashboardPage = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();

    const handleLogout = () => {
      storage.removeToken();
      storage.removeUser();

      dispatch(logout());

      navigate("/login", { replace: true });
      
    };

  const auth = useSelector((state) => state.auth);

  console.log(auth);
  return (
    <div>
      <h1 className="text-3xl font-bold">
        Dashboard
      </h1>

      <p className="text-muted-foreground mt-2">
        Welcome to DocSaaS
      </p>
      <Button onClick={handleLogout}>
      Logout
    </Button>
    </div>
  );
};

export default DashboardPage;