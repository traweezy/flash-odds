import { memo } from "react";
import clsx from "clsx";

type SkeletonProps = {
  className?: string;
};

export const Skeleton = memo(({ className }: SkeletonProps) => (
  <div
    className={clsx("animate-pulse rounded-md bg-slate-800/80", className)}
  />
));

Skeleton.displayName = "Skeleton";
