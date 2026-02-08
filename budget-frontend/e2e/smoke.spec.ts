import { test, expect } from "@playwright/test";

test("home page shows budget summary and quick actions", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { name: "Home Budget Tracker" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Budget Summary" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Create Budget" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Record Expense" })).toBeVisible();
  await expect(page.getByRole("button", { name: "View Categories" })).toBeVisible();
  await expect(page.getByRole("button", { name: "View Dashboard" })).toBeVisible();
});

test("budgets list and detail pages load", async ({ page }) => {
  await page.goto("/budgets");
  await expect(page.getByRole("heading", { name: "Budgets" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Create Budget" })).toBeVisible();

  await page.getByRole("button", { name: "View" }).first().click();
  await expect(page.getByRole("heading", { name: "Budget Overview" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Expenses" })).toBeVisible();
});

test("categories page loads", async ({ page }) => {
  await page.goto("/categories");
  await expect(page.getByRole("heading", { name: "Spending Categories" })).toBeVisible();
});

test("expense form loads for a budget", async ({ page }) => {
  await page.goto("/budgets");
  await page.getByRole("button", { name: "View" }).first().click();
  await page.getByRole("button", { name: "Add Expense" }).click();
  await expect(page).toHaveURL(/\/expenses\/new/);
  await expect(page.getByRole("heading", { name: "Record Expense" })).toBeVisible();
});

test("navigation provides breadcrumbs for nested pages", async ({ page }) => {
  await page.goto("/budgets/new");
  const breadcrumbs = page.getByRole("navigation", { name: "breadcrumbs" });
  await expect(breadcrumbs).toBeVisible();
  await expect(breadcrumbs).toContainText("Home");
  await expect(breadcrumbs).toContainText("Budgets");
  await expect(breadcrumbs).toContainText("New Budget");
});
