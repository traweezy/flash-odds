import { describe, expect, it } from "vitest";

import { formatAmericanOdds, formatLine } from "./odds";

describe("formatAmericanOdds", () => {
  it("formats positive odds with plus sign", () => {
    expect(formatAmericanOdds(120)).toBe("+120");
  });

  it("keeps negative odds unchanged", () => {
    expect(formatAmericanOdds(-115)).toBe("-115");
  });

  it("handles null as em dash", () => {
    expect(formatAmericanOdds(null)).toBe("—");
  });
});

describe("formatLine", () => {
  it("rounds to the nearest half point", () => {
    expect(formatLine(2.74)).toBe("2.7");
  });

  it("returns em dash when undefined", () => {
    expect(formatLine(null)).toBe("—");
  });
});
