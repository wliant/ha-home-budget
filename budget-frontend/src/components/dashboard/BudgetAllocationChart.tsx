'use client';

import React from 'react';
import { PieChart, Pie, Cell, ResponsiveContainer, Legend, Tooltip } from 'recharts';
import { Box, Typography } from '@mui/material';
import { useTheme } from '@mui/material/styles';
import { formatCurrency } from '@/services/budgetService';

const CHART_COLORS = [
  '#2196F3', '#4CAF50', '#FF9800', '#E91E63', '#9C27B0',
  '#00BCD4', '#FF5722', '#795548', '#607D8B', '#3F51B5',
  '#8BC34A', '#FFC107', '#F44336', '#009688', '#673AB7',
  '#CDDC39', '#FF6F00', '#1B5E20', '#880E4F', '#311B92',
];

export interface BudgetAllocationItem {
  name: string;
  icon?: string;
  amount: number;
}

interface BudgetAllocationChartProps {
  data: BudgetAllocationItem[];
  totalBudget: number;
  label?: string;
}

export default function BudgetAllocationChart({ data, totalBudget, label = 'total budget' }: BudgetAllocationChartProps) {
  const theme = useTheme();

  if (data.length === 0 || totalBudget === 0) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 300, p: 4 }}>
        <Typography color="text.secondary" variant="body1" textAlign="center">
          No budget data for this period.
        </Typography>
      </Box>
    );
  }

  const chartData = data
    .filter(d => d.amount > 0)
    .sort((a, b) => b.amount - a.amount);

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const renderCustomLabel = ({ cx, cy }: any) => {
    return (
      <text x={cx} y={cy} textAnchor="middle" dominantBaseline="central">
        <tspan x={cx} dy="-0.4em" fontSize="18" fontWeight="bold">
          {formatCurrency(totalBudget)}
        </tspan>
        <tspan x={cx} dy="1.4em" fontSize="12" fill={theme.palette.text.secondary}>
          {label}
        </tspan>
      </text>
    );
  };

  return (
    <Box sx={{ width: '100%', height: 350 }}>
      <ResponsiveContainer>
        <PieChart>
          <Pie
            data={chartData}
            cx="50%"
            cy="50%"
            innerRadius={65}
            outerRadius={100}
            dataKey="amount"
            nameKey="name"
            startAngle={90}
            endAngle={-270}
            labelLine={false}
            label={renderCustomLabel}
          >
            {chartData.map((entry, index) => (
              <Cell key={entry.name} fill={CHART_COLORS[index % CHART_COLORS.length]} />
            ))}
          </Pie>
          <Tooltip
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            formatter={(value: any) => formatCurrency(Number(value))}
          />
          <Legend
            formatter={(value: string) => (
              <Typography component="span" variant="caption" color="text.secondary">
                {value}
              </Typography>
            )}
          />
        </PieChart>
      </ResponsiveContainer>
    </Box>
  );
}
