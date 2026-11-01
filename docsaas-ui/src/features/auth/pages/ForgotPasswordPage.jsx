import { useState } from "react";
import { Link } from "react-router-dom";
import { ArrowLeft, MailCheck } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import usePageTitle from "@/hooks/usePageTitle";
import { useForgotPasswordMutation } from "../api/authApi";

const schema = z.object({ email: z.email("Please enter a valid email address") });

const ForgotPasswordPage = () => {
  usePageTitle("Forgot password");
  const [sentTo, setSentTo] = useState(null);
  const [serverError, setServerError] = useState(null);
  const [requestReset, { isLoading }] = useForgotPasswordMutation();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({ resolver: zodResolver(schema), defaultValues: { email: "" } });

  const onSubmit = async ({ email }) => {
    setServerError(null);
    try {
      await requestReset(email.trim()).unwrap();
      setSentTo(email.trim());
    } catch (error) {
      setServerError(error?.data?.message || "We couldn't send the email. Please try again.");
    }
  };

  if (sentTo) {
    return (
      <div className="w-full text-center">
        <span className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-primary/10 text-primary">
          <MailCheck size={26} />
        </span>
        <h1 className="mt-5 text-2xl font-semibold tracking-tight">Check your email</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          If an account exists for <span className="font-medium text-foreground">{sentTo}</span>, we've sent a link to reset
          the password. It works once and expires in 30 minutes.
        </p>
        <p className="mt-4 text-sm text-muted-foreground">Can't find it? Check your spam folder, or try again in a minute.</p>
        <div className="mt-6 grid gap-2">
          <Button variant="outline" className="h-11 rounded-xl" onClick={() => setSentTo(null)}>
            Use a different email
          </Button>
          <Button asChild className="h-11 rounded-xl">
            <Link to="/login">Back to log in</Link>
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="w-full">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold tracking-tight">Forgot your password?</h1>
        <p className="mt-1.5 text-sm text-muted-foreground">
          Enter the email you signed up with and we'll send you a link to choose a new one.
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <div className="space-y-2">
          <label htmlFor="forgot-email" className="text-sm font-medium">
            Email address
          </label>
          <Input
            id="forgot-email"
            {...register("email")}
            type="email"
            autoComplete="email"
            autoFocus
            placeholder="john@example.com"
            className="h-12 rounded-xl bg-background/60 px-4 text-base"
          />
          {errors.email && <p className="text-sm text-destructive">{errors.email.message}</p>}
        </div>

        {serverError && (
          <p role="alert" className="rounded-xl bg-destructive/10 px-3 py-2 text-sm text-destructive">
            {serverError}
          </p>
        )}

        <Button
          type="submit"
          disabled={isLoading}
          className="h-12 w-full rounded-xl text-base shadow-[0_10px_24px_-10px_var(--primary)]"
        >
          {isLoading ? "Sending link…" : "Send reset link"}
        </Button>
      </form>

      <p className="mt-6 text-center text-sm">
        <Link to="/login" className="inline-flex items-center gap-1.5 font-medium text-primary hover:underline">
          <ArrowLeft size={15} />
          Back to log in
        </Link>
      </p>
    </div>
  );
};

export default ForgotPasswordPage;
