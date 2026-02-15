'use client';

import React from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { Box, Typography } from '@mui/material';
import { useTheme } from '@mui/material/styles';

const CHART_COLORS = [
  '#2196F3', '#4CAF50', '#FF9800', '#E91E63', '#9C27B0',
  '#00BCD4', '#FF5722', '#795548', '#607D8B', '#3F51B5',
  '#8BC34A', '#FFC107', '#F44336', '#009688', '#673AB7',
  '#CDDC39', '#FF6F00', '#1B5E20', '#880E4F', '#311B92',
];

export interface SpendingTrendChartProps {
  chartData: Record<string, unknown>[];
  xAxisKey: string;
  categories: string[];
  xAxisFormatter?: (value: unknown) => string;
  granularity?: 'daily' | 'monthly' | 'yearly';
  hiddenCategories?: Set<string>;
  onToggleCategory?: (categoryName: string) => void;
}

export default function SpendingTrendChart({
  chartData,
  xAxisKey,
  categories,
  xAxisFormatter,
  granularity = 'monthly',
  hiddenCategories,
  onToggleCategory,
}: SpendingTrendChartProps) {
  const theme = useTheme();

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  };

  const formatTooltipLabel = (label: unknown) => {
    if (xAxisFormatter) return xAxisFormatter(label);
    return String(label);
  };

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const handleLegendClick = (data: any) => {
    if (onToggleCategory && data.dataKey) {
      onToggleCategory(String(data.dataKey));
    }
  };

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const renderLegend = (props: any) => {
    const { payload } = props;
    if (!payload) return null;

    return (
      <Box sx={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'center', gap: 1.5, mt: 1 }}>
        {payload.map((entry: { value: string; color: string }) => {
          const isHidden = hiddenCategories?.has(entry.value);
          return (
            <Box
              key={entry.value}
              onClick={() => onToggleCategory?.(entry.value)}
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 0.5,
                cursor: onToggleCategory ? 'pointer' : 'default',
                opacity: isHidden ? 0.4 : 1,
                textDecoration: isHidden ? 'line-through' : 'none',
                '&:hover': onToggleCategory ? { opacity: isHidden ? 0.6 : 0.8 } : {},
              }}
            >
              <Box
                sx={{
                  width: 12,
                  height: 12,
                  borderRadius: '50%',
                  backgroundColor: entry.color,
                }}
              />
              <Typography variant="caption" color="text.secondary">
                {entry.value}
              </Typography>
            </Box>
          );
        })}
      </Box>
    );
  };

  if (chartData.length === 0) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 400, p: 4 }}>
        <Typography color="text.secondary" variant="body1" textAlign="center">
          No expense data available. Start adding expenses to see spending trends.
        </Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ width: '100%', height: { xs: 350, sm: 450 } }}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={chartData} margin={{ top: 5, right: 20, left: 10, bottom: 5 }}>
          <CartesianGrid strokeDasharray="3 3" stroke={theme.palette.divider} />
          <XAxis
            dataKey={xAxisKey}
            tickFormatter={xAxisFormatter}
            tick={{ fontSize: 12, fill: theme.palette.text.secondary }}
            stroke={theme.palette.divider}
          />
          <YAxis
            tickFormatter={(v) => formatCurrency(v)}
            tick={{ fontSize: 12, fill: theme.palette.text.secondary }}
            stroke={theme.palette.divider}
            width={80}
          />
          <Tooltip
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            formatter={(value: any) => [formatCurrency(Number(value))]}
            labelFormatter={formatTooltipLabel}
            contentStyle={{
              backgroundColor: theme.palette.background.paper,
              border: `1px solid ${theme.palette.divider}`,
              borderRadius: 8,
            }}
          />
          <Legend content={renderLegend} onClick={handleLegendClick} />
          {categories.map((category, index) => (
            <Line
              key={category}
              type="monotone"
              dataKey={category}
              stroke={CHART_COLORS[index % CHART_COLORS.length]}
              strokeWidth={2}
              dot={{ r: 3 }}
              activeDot={{ r: 5 }}
              hide={hiddenCategories?.has(category)}
              connectNulls={false}
            />
          ))}
        </LineChart>
      </ResponsiveContainer>
    </Box>
  );
}
