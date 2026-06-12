//import { FcGoogle } from "react-icons/fc";
import { useState } from "react";
import { Eye, EyeOff } from "lucide-react";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { loginSchema } from "../validation/loginSchema";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useLoginMutation } from "@/features/auth/api/authApi";
import { useDispatch } from "react-redux";
import { setCredentials } from "@/features/auth/slice/authSlice";
import { storage } from "@/lib/storage";
import { AUTH_PROVIDER } from "@/constants/authProvider";
import toast from "react-hot-toast";
import { useNavigate } from "react-router-dom";
import { GoogleLogin } from "@react-oauth/google";
import { useGoogleLoginMutation } from "@/features/auth/api/authApi";
import { useGoogleLogin } from "@react-oauth/google";

const LoginForm = () => {
  const [showPassword, setShowPassword] = useState(false);

  const {
    register,
    handleSubmit,
    resetField,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(loginSchema),

    defaultValues: {
      email: "",
      password: "",
    },
  });

  const [loginUser, { isLoading }] = useLoginMutation();
  const [googleLoginApi] = useGoogleLoginMutation();

  // const onSubmit = async (data) => {
  //   console.log("Login Data:", data);
  // };

  

  const dispatch = useDispatch();
  const navigate = useNavigate();

  const onSubmit = async (data) => {
  try {
    const response = await loginUser(data).unwrap();

    if (!response.success) {
      resetField("password");
      toast.error(response.message);
      return;
    }

    storage.setToken(response.data.token);
    storage.setUser(response.data.user);

    dispatch(
      setCredentials({
        user: response.data.user,
        accessToken: response.data.token,
        provider: AUTH_PROVIDER.LOCAL,
      })
    );

    toast.success("Login successful");

    navigate("/dashboard", { replace: true });

  } catch (error) {
    resetField("password");
    toast.error(
      error?.data?.message || "Login failed"
    );

    console.error("Login Error:", error);
  }
};


  


  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      className="space-y-5"
    >

      

      {/* Google Login */}
      <div className="flex justify-center ">
  <GoogleLogin
    theme="outline"
    size="large"
    text="signin_with"
    onSuccess={async (credentialResponse) => {

      const loadingToast = toast.loading(
        "Signing in with Google..."
      );

      try {

       const response = await googleLoginApi(
            credentialResponse.credential
          ).unwrap();


          if (!response.success) {

            toast.dismiss(loadingToast);

            toast.error(response.message);

            if (
              response.message ===
              "User not registered. Please create an account first."
            ) {
              setTimeout(() => {
                navigate("/register");
              }, 1500);
            }

            return;
          }

          toast.dismiss(loadingToast);

          storage.setToken(response.data.token);
          storage.setUser(response.data.user);

          dispatch(
            setCredentials({
              user: response.data.user,
              accessToken: response.data.token,
              provider: AUTH_PROVIDER.GOOGLE,
            })
          );

          toast.success("Login successful");

          navigate("/dashboard", {
            replace: true,
          });

      } catch (error) {

          toast.dismiss(loadingToast);

          const message =
            error?.data?.message ||
            "Google login failed";

          toast.error(message);

         if (
              message ===
              "User not registered. Please create an account first."
            )
            {
              setTimeout(() => {
                navigate("/register");
              }, 1500);
            }

          console.error(error);
        }
    }}
    onError={() => {
      toast.error(
        "Google login failed"
      );
    }}
  />
</div>

      {/* Divider */}
      <div className="relative">
        <div className="absolute inset-0 flex items-center">
          <span className="w-full border-t" />
        </div>

        <div className="relative flex justify-center text-xs uppercase">
          <span className="bg-card px-3 text-muted-foreground">
            Or continue with email
          </span>
        </div>
      </div>

      {/* Email */}
      <div className="space-y-2">
        <label className="text-sm font-medium">
          Email Address
        </label>

        <Input
          {...register("email")}
          type="email"
          placeholder="john@example.com"
          className="h-14 px-5 text-base"
        />

        {errors.email && (
          <p className="text-sm text-red-500">
            {errors.email.message}
          </p>
        )}
      </div>

      {/* Password */}
      <div className="space-y-2">
        <div className="flex items-center justify-between">
          <label className="text-sm font-medium">
            Password
          </label>

          <button
            type="button"
            className="text-sm text-blue-600 hover:text-blue-700"
          >
            Forgot Password?
          </button>
        </div>

        <div className="relative">
          <Input
            {...register("password")}
            type={showPassword ? "text" : "password"}
            placeholder="Enter your password"
            className="h-14 px-5 pr-12 text-base"
          />

          <button
            type="button"
            onClick={() => setShowPassword(!showPassword)}
            className="absolute right-4 top-1/2 -translate-y-1/2 text-muted-foreground"
          >
            {showPassword ? (
              <EyeOff size={20} />
            ) : (
              <Eye size={20} />
            )}
          </button>
        </div>

        {errors.password && (
          <p className="text-sm text-red-500">
            {errors.password.message}
          </p>
        )}
      </div>

      <Button
  type="submit"
  disabled={isSubmitting || isLoading}
  className="h-14 w-full bg-blue-600 dark:text-white text-base font-semibold hover:bg-blue-700"
>
  {isSubmitting || isLoading
    ? "Signing In..."
    : "Sign In"}
</Button>
    </form>
  );
};

export default LoginForm;