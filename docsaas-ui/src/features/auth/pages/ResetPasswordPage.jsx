import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useDispatch } from "react-redux";
import { CircleAlert, Eye, EyeOff, Loader2 } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import toast from "react-hot-toast";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import usePageTitle from "@/hooks/usePageTitle";
import { logout } from "@/features/auth/slice/authSlice";
import { storage } from "@/lib/storage";
import { useResetPasswordMutation, useVerifyResetTokenQuery } from "../api/authApi";

const schema = z
  .object({
    password: z.string().min(6, "Password must be at least 6 characters").max(100, "Password must be at most 100 characters"),
    confirm: z.string(),
  })
  .refine((v) => v.password === v.confirm, { message: "Passwords don't match", path: ["confirm"] });

const InvalidLink = () => (
  <div className="w-full text-center">
    <span className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-destructive/10 text-destructive">
      <CircleAlert size={26} />
    </span>
    <h1 className="mt-5 text-2xl font-semibold tracking-tight">This link has expired</h1>
    <p className="mt-2 text-sm text-muted-foreground">
      Reset links work once and expire after 30 minutes. Request a new one and use the latest email.
    </p>
    <Button asChild className="mt-6 h-11 w-full rounded-xl">
      <Link to="/forgot-password">Send a new link</Link>
    </Button>
  </div>
);

const ResetPasswordPage = () => {
  usePageTitle("Choose a new password");
  const [params] = useSearchParams();
  const token = params.get("token") ?? "";
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const [show, setShow] = useState(false);
  const [serverError, setServerError] = useState(null);

  const { data: check, isLoading: checking } = useVerifyResetTokenQuery(token, { skip: !token });
  const [resetPassword, { isLoading }] = useResetPasswordMutation();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({ resolver: zodResolver(schema), defaultValues: { password: "", confirm: "" } });

  if (!token || check?.valid === false) return <InvalidLink />;
  if (checking) return <Loader2 className="mx-auto my-10 animate-spin text-muted-foreground" />;

  const onSubmit = async ({ password }) => {
    setServerError(null);
    try {
      await resetPassword({ token, newPassword: password }).unwrap();
      // A session in this browser may belong to the old password; start fresh.
      storage.removeToken();
      storage.removeUser();
      dispatch(logout());
      toast.success("Password changed. Log in with your new password.");
      navigate("/login", { replace: true });
    } catch (error) {
      setServerError(error?.data?.message || "We couldn't change your password. Please try again.");
    }
  };

  return (
    <div className="w-full">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold tracking-tight">Choose a new password</h1>
        <p className="mt-1.5 text-sm text-muted-foreground">Use at least 6 characters. You'll log in with it next.</p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <div className="space-y-2">
          <label htmlFor="new-password" className="text-sm font-medium">
            New password
          </label>
          <div className="relative">
            <Input
              id="new-password"
              {...register("password")}
              type={show ? "text" : "password"}
              autoComplete="new-password"
              autoFocus
              className="h-12 rounded-xl bg-background/60 px-4 pr-12 text-base"
            />
            <button
              type="button"
              onClick={() => setShow(!show)}
              aria-label={show ? "Hide password" : "Show password"}
              className="absolute right-4 top-1/2 -translate-y-1/2 text-muted-foreground"
            >
              {show ? <EyeOff size={20} /> : <Eye size={20} />}
            </button>
          </div>
          {errors.password && <p className="text-sm text-destructive">{errors.password.message}</p>}
        </div>

        <div className="space-y-2">
          <label htmlFor="confirm-password" className="text-sm font-medium">
            Confirm new password
          </label>
          <Input
            id="confirm-password"
            {...register("confirm")}
            type={show ? "text" : "password"}
            autoComplete="new-password"
            className="h-12 rounded-xl bg-background/60 px-4 text-base"
          />
          {errors.confirm && <p className="text-sm text-destructive">{errors.confirm.message}</p>}
        </div>

        {serverError && (
          <p role="alert" className="rounded-xl bg-destructive/10 px-3 py-2 text-sm text-destructive">
            {serverError}{" "}
            <Link to="/forgot-password" className="font-medium underline">
              Send a new link
            </Link>
          </p>
        )}

        <Button
          type="submit"
          disabled={isLoading}
          className="h-12 w-full rounded-xl text-base shadow-[0_10px_24px_-10px_var(--primary)]"
        >
          {isLoading ? "Saving…" : "Save new password"}
        </Button>
      </form>
    </div>
  );
};

export default ResetPasswordPage;
