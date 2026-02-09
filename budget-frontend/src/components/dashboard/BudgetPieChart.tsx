'use client';

import React from 'react';
import { PieChart, Pie, Cell, ResponsiveContainer, Legend, Tooltip } from 'recharts';
import { Box, Typography } from '@mui/material';
import { useTheme } from '@mui/material/styles';
import { formatCurrency } from '@/services/budgetService';

interface BudgetPieChartProps {
  totalBudget: number;
  totalSpending: number;
}

export default function BudgetPieChart({ totalBudget, totalSpending }: BudgetPieChartProps) {
  const theme = useTheme();

  const COLORS = {
    spent: theme.palette.primary.main,
    remaining: theme.palette.grey[300],
    overBudget: theme.palette.error.main,
    budgetPortion: theme.palette.warning.main,
  };

  const isOverBudget = totalSpending > totalBudget;

  let data: { name: string; value: number; color: string }[];

  if (totalBudget === 0 && totalSpending === 0) {
    data = [{ name: 'No Budget', value: 1, color: COLORS.remaining }];
  } else if (isOverBudget) {
    const overAmount = totalSpending - totalBudget;
    data = [
      { name: 'Budget', value: totalBudget, color: COLORS.budgetPortion },
      { name: 'Over Budget', value: overAmount, color: COLORS.overBudget },
    ];
  } else {
    const remaining = totalBudget - totalSpending;
    data = [
      { name: 'Spent', value: totalSpending, color: COLORS.spent },
      { name: 'Remaining', value: remaining, color: COLORS.remaining },
    ];
  }

  const renderCustomLabel = ({ cx, cy }: any) => {
    const percentage = totalBudget > 0 ? ((totalSpending / totalBudget) * 100).toFixed(1) : '0.0';
    return (
      <text x={cx} y={cy} textAnchor="middle" dominantBaseline="central">
        <tspan x={cx} dy="-0.4em" fontSize="20" fontWeight="bold">
          {percentage}%
        </tspan>
        <tspan x={cx} dy="1.4em" fontSize="12" fill={theme.palette.text.secondary}>
          used
        </tspan>
      </text>
    );
  };

  return (
    <Box sx={{ width: '100%', height: 220 }}>
      <ResponsiveContainer>
        <PieChart>
          <Pie
            data={data}
            cx="50%"
            cy="50%"
            innerRadius={55}
            outerRadius={80}
            dataKey="value"
            startAngle={90}
            endAngle={-270}
            labelLine={false}
            label={renderCustomLabel}
          >
            {data.map((entry, index) => (
              <Cell key={index} fill={entry.color} />
            ))}
          </Pie>
          <Tooltip
            formatter={(value) => formatCurrency(Number(value))}
          />
          <Legend
            formatter={(value: string, entry: any) => (
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
