import { memo } from "react";

export type SelectOption = {
  value: string;
  label: string;
};

type Props = {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  options: SelectOption[];
};

export const SelectField = memo(
  ({ id, label, value, onChange, options }: Props) => (
    <label className="flex flex-col gap-1 text-sm text-slate-300" htmlFor={id}>
      <span className="font-medium text-slate-200">{label}</span>
      <select
        id={id}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100 focus:outline-none focus:ring-2 focus:ring-accent"
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </label>
  ),
);

SelectField.displayName = "SelectField";
