import { memo } from "react";

type Props = {
  id: string;
  label: string;
  value: string;
  placeholder?: string;
  onChange: (value: string) => void;
};

export const InputField = memo(
  ({ id, label, value, placeholder, onChange }: Props) => (
    <label className="flex flex-col gap-1 text-sm text-slate-300" htmlFor={id}>
      <span className="font-medium text-slate-200">{label}</span>
      <input
        id={id}
        type="search"
        value={value}
        placeholder={placeholder}
        onChange={(event) => onChange(event.target.value)}
        className="rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100 focus:outline-none focus:ring-2 focus:ring-accent"
      />
    </label>
  ),
);

InputField.displayName = "InputField";
