import { useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Loader2 } from "lucide-react";
import toast from "react-hot-toast";

import usePageTitle from "@/hooks/usePageTitle";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { storage } from "@/lib/storage";
import { selectUser } from "@/features/auth/selectors/authSelectors";
import { logout, updateUser } from "@/features/auth/slice/authSlice";
import {
  apiErrorMessage,
  useChangePasswordMutation,
  useDeleteAccountMutation,
  useUpdateProfileMutation,
} from "@/features/account/api/accountApi";

const profileSchema = z.object({
  fullName: z.string().trim().min(1, "Full name is required").max(150, "Full name must be at most 150 characters"),
});

const passwordSchema = z
  .object({
    currentPassword: z.string().min(1, "Current password is required"),
    newPassword: z.string().min(6, "Password must be at least 6 characters").max(100, "Password is too long"),
    confirmPassword: z.string(),
  })
  .refine((v) => v.newPassword === v.confirmPassword, { path: ["confirmPassword"], message: "Passwords do not match" })
  .refine((v) => v.newPassword !== v.currentPassword, { path: ["newPassword"], message: "Choose a different password" });

const Section = ({ title, description, children, danger }) => (
  <section className={`glass rounded-3xl p-6 ${danger ? "border-destructive/30" : ""}`}>
    <h2 className={`font-semibold ${danger ? "text-destructive" : ""}`}>{title}</h2>
    {description && <p className="mt-1 text-sm text-muted-foreground">{description}</p>}
    <div className="mt-5">{children}</div>
  </section>
);

const Field = ({ id, label, error, ...props }) => (
  <div className="space-y-1.5">
    <Label htmlFor={id}>{label}</Label>
    <Input id={id} aria-invalid={!!error} aria-describedby={error ? `${id}-error` : undefined} className="h-11 rounded-xl" {...props} />
    {error && (
      <p id={`${id}-error`} role="alert" className="text-sm text-destructive">
        {error}
      </p>
    )}
  </div>
);

const ProfileForm = ({ user }) => {
  const dispatch = useDispatch();
  const [updateProfile] = useUpdateProfileMutation();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting, isDirty },
  } = useForm({ resolver: zodResolver(profileSchema), defaultValues: { fullName: user?.fullName ?? "" } });

  const onSubmit = async (values) => {
    try {
      const updated = await updateProfile(values).unwrap();
      dispatch(updateUser({ fullName: updated.fullName }));
      storage.setUser({ ...storage.getUser(), fullName: updated.fullName });
      reset({ fullName: updated.fullName });
      toast.success("Profile updated");
    } catch (err) {
      toast.error(apiErrorMessage(err));
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4 sm:max-w-md" noValidate>
      <Field id="fullName" label="Full name" autoComplete="name" error={errors.fullName?.message} {...register("fullName")} />
      <Field id="email" label="Email" value={user?.email ?? ""} disabled readOnly />
      <Button type="submit" disabled={isSubmitting || !isDirty} className="h-11 w-fit rounded-xl px-6">
        {isSubmitting && <Loader2 className="animate-spin" />}
        Save changes
      </Button>
    </form>
  );
};

const PasswordForm = () => {
  const [changePassword] = useChangePasswordMutation();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(passwordSchema),
    defaultValues: { currentPassword: "", newPassword: "", confirmPassword: "" },
  });

  const onSubmit = async ({ currentPassword, newPassword }) => {
    try {
      await changePassword({ currentPassword, newPassword }).unwrap();
      reset();
      toast.success("Password changed");
    } catch (err) {
      toast.error(apiErrorMessage(err));
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4 sm:max-w-md" noValidate>
      <Field id="currentPassword" type="password" label="Current password" autoComplete="current-password" error={errors.currentPassword?.message} {...register("currentPassword")} />
      <Field id="newPassword" type="password" label="New password" autoComplete="new-password" error={errors.newPassword?.message} {...register("newPassword")} />
      <Field id="confirmPassword" type="password" label="Confirm new password" autoComplete="new-password" error={errors.confirmPassword?.message} {...register("confirmPassword")} />
      <Button type="submit" disabled={isSubmitting} className="h-11 w-fit rounded-xl px-6">
        {isSubmitting && <Loader2 className="animate-spin" />}
        Change password
      </Button>
    </form>
  );
};

const DeleteAccount = ({ user }) => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [deleteAccount, { isLoading }] = useDeleteAccountMutation();
  const [confirmEmail, setConfirmEmail] = useState("");
  const matches = confirmEmail.trim().toLowerCase() === user?.email?.toLowerCase();

  const onDelete = async (event) => {
    event.preventDefault();
    try {
      await deleteAccount({ confirmEmail: confirmEmail.trim() }).unwrap();
      storage.removeToken();
      storage.removeUser();
      dispatch(logout());
      toast.success("Your account was deleted");
      navigate("/");
    } catch (err) {
      toast.error(apiErrorMessage(err));
    }
  };

  return (
    <form onSubmit={onDelete} className="grid gap-4 sm:max-w-md">
      <Field
        id="confirmEmail"
        label={`Type ${user?.email ?? "your email"} to confirm`}
        value={confirmEmail}
        onChange={(e) => setConfirmEmail(e.target.value)}
        autoComplete="off"
      />
      <Button type="submit" variant="destructive" disabled={!matches || isLoading} className="h-11 w-fit rounded-xl px-6">
        {isLoading && <Loader2 className="animate-spin" />}
        Delete my account
      </Button>
    </form>
  );
};

const SettingsPage = () => {
  usePageTitle("Settings");
  const user = useSelector(selectUser);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Settings</h1>
        <p className="mt-1 text-sm text-muted-foreground">Manage your profile, password and account.</p>
      </div>

      <Section title="Profile">
        <ProfileForm user={user} />
      </Section>

      <Section
        title="Password"
        description="Signed up with Google? Your account has no password to change; keep signing in with Google."
      >
        <PasswordForm />
      </Section>

      <Section
        danger
        title="Delete account"
        description="Permanently deletes your account, job history, stored results and credits. This cannot be undone."
      >
        <DeleteAccount user={user} />
      </Section>
    </div>
  );
};

export default SettingsPage;
