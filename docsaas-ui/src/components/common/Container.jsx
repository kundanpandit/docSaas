import { cn } from "@/lib/utils";

/** Centered content column with side padding, so nothing stretches edge to edge. */
const Container = ({ className, children, as: Tag = "div", ...props }) => (
  <Tag className={cn("mx-auto w-full max-w-6xl px-4 sm:px-6 lg:px-8", className)} {...props}>
    {children}
  </Tag>
);

export default Container;
