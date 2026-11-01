import { LayoutGroup, motion } from "framer-motion";

import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

const ChoiceOption = ({ option, value, disabled, onChange }) => (
  <fieldset disabled={disabled}>
    <legend className="mb-2.5 text-sm font-medium">{option.label}</legend>

    <LayoutGroup id={`option-${option.name}`}>
      <div className={cn("grid gap-2", option.choices.length > 3 ? "grid-cols-2" : "grid-cols-3")}>
        {option.choices.map((choice) => {
          const selected = value === choice.value;

          return (
            <label
              key={choice.value}
              className={cn(
                "relative cursor-pointer rounded-xl border px-2.5 py-2.5 transition-colors has-[:focus-visible]:ring-3 has-[:focus-visible]:ring-ring/50",
                selected ? "border-transparent" : "border-border bg-background/40 hover:border-primary/40",
                disabled && "cursor-not-allowed opacity-60"
              )}
            >
              {selected && (
                <motion.span
                  layoutId="selected"
                  transition={{ type: "spring", stiffness: 500, damping: 36 }}
                  className="absolute inset-0 rounded-xl bg-primary/10 ring-2 ring-primary"
                />
              )}
              <input
                type="radio"
                name={option.name}
                value={choice.value}
                checked={selected}
                onChange={() => onChange(option.name, choice.value)}
                className="sr-only"
              />
              <span className={cn("relative block truncate text-sm font-medium", selected && "text-primary")}>{choice.label}</span>
              {choice.hint && <span className="relative block text-xs text-muted-foreground">{choice.hint}</span>}
            </label>
          );
        })}
      </div>
    </LayoutGroup>
  </fieldset>
);

const TextOption = ({ option, value, error, disabled, onChange }) => (
  <div className="space-y-2">
    <label htmlFor={`option-${option.name}`} className="text-sm font-medium">
      {option.label}
    </label>
    <Input
      id={`option-${option.name}`}
      type={option.inputType ?? "text"}
      autoComplete={option.inputType === "password" ? "new-password" : undefined}
      value={value}
      disabled={disabled}
      placeholder={option.placeholder}
      onChange={(event) => onChange(option.name, event.target.value)}
      aria-invalid={Boolean(error)}
      aria-describedby={`option-${option.name}-hint`}
      className="h-11 rounded-xl bg-background/60 px-4"
    />
    <p id={`option-${option.name}-hint`} className={cn("text-xs", error ? "text-destructive" : "text-muted-foreground")}>
      {error || option.hint}
    </p>
  </div>
);

const ToolOptions = ({ options, values, errors, disabled, onChange }) => {
  const visible = options.filter((option) => !option.showIf || option.showIf(values));

  if (visible.length === 0) {
    return null;
  }

  return (
    <div className="space-y-6">
      {visible.map((option) =>
        option.type === "choice" ? (
          <ChoiceOption key={option.name} option={option} value={values[option.name]} disabled={disabled} onChange={onChange} />
        ) : (
          <TextOption
            key={option.name}
            option={option}
            value={values[option.name]}
            error={errors[option.name]}
            disabled={disabled}
            onChange={onChange}
          />
        )
      )}
    </div>
  );
};

export default ToolOptions;
