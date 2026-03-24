import type { CategoryExpenseAggregate } from '@/services/expenseService';

export type Granularity = 'daily' | 'monthly' | 'yearly';

export const MONTH_NAMES = [
  'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
  'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
];

export const MONTH_FULL_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

export function formatMonthLabel(value: unknown): string {
  const num = Number(value);
  if (num >= 1 && num <= 12) return MONTH_NAMES[num - 1];
  return String(value);
}

export function formatDayLabel(value: unknown): string {
  return String(Number(value));
}

export function formatYearLabel(value: unknown): string {
  return String(Number(value));
}

export interface TransformedData {
  chartData: Record<string, unknown>[];
  categories: string[];
}

export function transformAggregates(
  aggregates: CategoryExpenseAggregate[],
  granularity: Granularity,
  daysInMonth?: number,
  maxMonth?: number,
): TransformedData {
  const categories = new Set<string>();
  const bucketMap: Record<number, Record<string, number>> = {};

  for (const agg of aggregates) {
    categories.add(agg.categoryName);
    let bucket: number;
    if (granularity === 'daily') {
      bucket = agg.day ?? 0;
    } else if (granularity === 'monthly') {
      bucket = agg.month ?? 0;
    } else {
      bucket = agg.year;
    }
    if (!bucketMap[bucket]) bucketMap[bucket] = {};
    bucketMap[bucket][agg.categoryName] = agg.totalAmount;
  }

  const categoryList = Array.from(categories).sort();
  const chartData: Record<string, unknown>[] = [];

  if (granularity === 'daily') {
    const days = daysInMonth ?? 31;
    for (let d = 1; d <= days; d++) {
      const point: Record<string, unknown> = { day: d };
      for (const cat of categoryList) {
        point[cat] = bucketMap[d]?.[cat] ?? 0;
      }
      chartData.push(point);
    }
  } else if (granularity === 'monthly') {
    const endMonth = maxMonth ?? 12;
    for (let m = 1; m <= endMonth; m++) {
      const point: Record<string, unknown> = { month: m };
      for (const cat of categoryList) {
        point[cat] = bucketMap[m]?.[cat] ?? 0;
      }
      chartData.push(point);
    }
  } else {
    // Yearly: use only years that exist in data
    const years = Object.keys(bucketMap).map(Number).sort();
    for (const y of years) {
      const point: Record<string, unknown> = { year: y };
      for (const cat of categoryList) {
        point[cat] = bucketMap[y]?.[cat] ?? 0;
      }
      chartData.push(point);
    }
  }

  return { chartData, categories: categoryList };
}

export function getDaysInMonth(year: number, month: number): number {
  return new Date(year, month, 0).getDate();
}
