//import { FcGoogle } from "react-icons/fc";
import { useState } from "react";
import { Eye, EyeOff } from "lucide-react";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { registerSchema } from "../validation/registerSchema";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useRegisterMutation , useGoogleRegisterMutation} from "@/features/auth/api/authApi";
import { useNavigate } from "react-router-dom";
//import { useGoogleLogin } from "@react-oauth/google";
import toast from "react-hot-toast";

import { GoogleLogin } from "@react-oauth/google";




const RegisterForm = () => {
  const [showPassword, setShowPassword] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(registerSchema),

    defaultValues: {
      name: "",
      email: "",
      password: "",
      confirmPassword: "",
    },
  });

  const navigate = useNavigate();

 const [registerUser, { isLoading }] = useRegisterMutation();
const [googleRegisterApi] =
  useGoogleRegisterMutation();

 const onSubmit = async (data) => {
  const payload = {
    fullName: data.name,
    email: data.email,
    password: data.password,
  };

  const loadingToast = toast.loading(
    "Creating your account..."
  );

  try {
    const response = await registerUser(payload).unwrap();

    toast.dismiss(loadingToast);

   toast.success(
  "Account created successfully. Please login."
);

reset();


navigate("/login");

  } catch (error) {

    toast.dismiss(loadingToast);

    toast.error(
      error?.data?.message || "Registration failed"
    );

    console.error("Register Error:", error);
  }
};

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      className="space-y-5"
    >

    {/* <Button
      type="button"
      variant="outline"
      className="h-14 w-full text-base font-medium"
    >
      <FcGoogle className="mr-2 h-5 w-5" />
      Continue with Google
    </Button> */}

    <div className="flex justify-center">
      <div className="space-y-3">

  <GoogleLogin
    theme="outline"
    size="large"
    text="signup_with"
    
    onSuccess={async (credentialResponse) => {

      const loadingToast = toast.loading(
        "Creating account with Google..."
      );

      try {

        const response = await googleRegisterApi(
            credentialResponse.credential
          ).unwrap();

          toast.dismiss(loadingToast);

          if (!response.success) {

            toast.error(
              response.message
            );

            setTimeout(() => {
              navigate("/login");
            }, 1500);

            return;
          }

          toast.success(
            "Account created successfully. Please login."
          );

          setTimeout(() => {
            navigate("/login");
          }, 1500);
         

        /*
         * TODO
         * Same logic as email login:
         *
         * save token
         * save user
         * dispatch auth state
         * redirect dashboard
         * 
         * by kundan kumar
         */

      } catch (error) {

        toast.dismiss(loadingToast);

       toast.error(
        error?.data?.message ||
        error?.message ||
        "Google signup failed"
      );
       
      }
    }}
    onError={() => {
      toast.error("Google signup failed");
    }}
  />

</div>
    </div>

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

      {/* Full Name */}
      <div className="space-y-2">
        <label className="text-sm font-medium">
          Full Name
        </label>

        <Input
          {...register("name")}
          placeholder="John Doe"
          className="h-14 px-5 text-base"
        />

        {errors.name && (
          <p className="text-sm text-red-500">
            {errors.name.message}
          </p>
        )}
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
        <label className="text-sm font-medium">
          Password
        </label>

        <div className="relative">
          <Input
            {...register("password")}
            type={showPassword ? "text" : "password"}
            placeholder="Create a strong password"
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

      {/* Confirm Password */}
      <div className="space-y-2">
        <label className="text-sm font-medium">
          Confirm Password
        </label>

        <Input
          {...register("confirmPassword")}
          type={showPassword ? "text" : "password"}
          placeholder="Confirm your password"
          className="h-14 px-5 text-base"
        />

        {errors.confirmPassword && (
          <p className="text-sm text-red-500">
            {errors.confirmPassword.message}
          </p>
        )}
      </div>

     <Button
        type="submit"
        disabled={isSubmitting || isLoading}
      className="h-14 w-full bg-blue-600 dark:text-white text-base font-semibold hover:bg-blue-700">
        {isLoading
          ? "Creating Account..."
          : "Create Account"}
      </Button>
      <p className="text-center text-xs text-muted-foreground">
        By creating an account, you agree to our
        Terms of Service and Privacy Policy.
      </p>
    </form>
  );
};

export default RegisterForm;