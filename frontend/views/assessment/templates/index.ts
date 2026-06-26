import { htmlCssIntro } from "./html-css-intro";
import { javaBasics } from "./java-basics";
import { sqlBasics } from "./sql-basics";
import type { TestTemplate } from "./types";

export type { TestTemplate };

export function getTestTemplates(): TestTemplate[] {
  return [javaBasics, sqlBasics, htmlCssIntro];
}
