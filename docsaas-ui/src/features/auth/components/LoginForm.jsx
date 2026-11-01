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
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useGetUsageQuery } from "@/features/usage/api/usageApi";
import { GoogleLogin } from "@react-oauth/google";
import { useGetAuthOptionsQuery, useGoogleLoginMutation } from "@/features/auth/api/authApi";

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
  const location = useLocation();
  // Guest numbers, to show how much logging in adds
  const { data: guestUsage } = useGetUsageQuery("guest");
  // "Forgot password?" only when the server can actually send the email
  const { data: authOptions } = useGetAuthOptionsQuery();

  /** Saves the session, celebrates the credit increase and returns to where the user came from. */
  const finishLogin = (response, provider) => {
    storage.setToken(response.data.token);
    storage.setUser(response.data.user);

    dispatch(
      setCredentials({
        user: response.data.user,
        accessToken: response.data.token,
        provider,
      })
    );

    const daily = guestUsage?.userDaily ?? 30;
    const before = guestUsage?.guestDaily ?? 3;
    const name = response.data.user?.fullName?.split(" ")[0];
    // clear any error toasts left from earlier failed attempts
    toast.dismiss();
    toast.success(
      `Welcome${name ? `, ${name}` : ""}! Your credits went up from ${before} to ${daily} a day, and AI tools are unlocked.`,
      { duration: 6000 }
    );

    navigate(location.state?.from || "/", { replace: true });
  };

  const onSubmit = async (data) => {
  try {
    const response = await loginUser(data).unwrap();

    if (!response.success) {
      resetField("password");
      toast.error(response.message);
      return;
    }

    finishLogin(response, AUTH_PROVIDER.LOCAL);

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
      className="space-y-4"
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

          finishLogin(response, AUTH_PROVIDER.GOOGLE);

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
      <div className="flex items-center gap-3 text-xs uppercase text-muted-foreground before:h-px before:flex-1 before:bg-border after:h-px after:flex-1 after:bg-border">
        Or continue with email
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
          className="h-12 rounded-xl bg-background/60 px-4 text-base"
        />

        {errors.email && (
          <p className="text-sm text-destructive">
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

          {authOptions?.passwordReset && (
            <Link
              to="/forgot-password"
              className="text-sm text-primary hover:underline"
            >
              Forgot password?
            </Link>
          )}
        </div>

        <div className="relative">
          <Input
            {...register("password")}
            type={showPassword ? "text" : "password"}
            placeholder="Enter your password"
            className="h-12 rounded-xl bg-background/60 px-4 pr-12 text-base"
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
          <p className="text-sm text-destructive">
            {errors.password.message}
          </p>
        )}
      </div>

      <Button
  type="submit"
  disabled={isSubmitting || isLoading}
  className="h-12 w-full rounded-xl text-base shadow-[0_10px_24px_-10px_var(--primary)]"
>
  {isSubmitting || isLoading
    ? "Signing In..."
    : "Sign In"}
</Button>
    </form>
  );
};

export default LoginForm;