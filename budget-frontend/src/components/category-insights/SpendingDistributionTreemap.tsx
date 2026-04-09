'use client';

import React, { useMemo } from 'react';
import { Treemap, ResponsiveContainer, Tooltip } from 'recharts';
import {
  Box,
  Typography,
  List,
  ListItemButton,
  ListItemText,
  ListItemIcon,
} from '@mui/material';
import { useTheme } from '@mui/material/styles';
import { formatCurrency } from '@/utils/formatters';
import type { CategoryExpenseAggregate } from '@/services/expenseService';

const CHART_COLORS = [
  '#2196F3', '#4CAF50', '#FF9800', '#E91E63', '#9C27B0',
  '#00BCD4', '#FF5722', '#795548', '#607D8B', '#3F51B5',
  '#8BC34A', '#FFC107', '#F44336', '#009688', '#673AB7',
  '#CDDC39', '#FF6F00', '#1B5E20', '#880E4F', '#311B92',
];

interface SpendingDistributionTreemapProps {
  aggregates: CategoryExpenseAggregate[];
  onCategoryClick?: (categoryId: number) => void;
  selectedCategoryId?: number | null;
}

interface TreemapItem {
  name: string;
  icon?: string;
  categoryId: number;
  size: number;
  color: string;
  [key: string]: unknown;
}

function CustomContent(props: any) {
  const { x, y, width, height, name, icon, color } = props;
  if (width < 4 || height < 4) return null;

  const showText = width > 60 && height > 30;
  const showAmount = width > 80 && height > 45;

  return (
    <g>
      <rect
        x={x}
        y={y}
        width={width}
        height={height}
        fill={color}
        stroke="#fff"
        strokeWidth={2}
        rx={4}
        style={{ cursor: 'pointer' }}
      />
      {showText && (
        <text
          x={x + width / 2}
          y={y + height / 2 - (showAmount ? 8 : 0)}
          textAnchor="middle"
          dominantBaseline="central"
          fill="#fff"
          fontSize={Math.min(14, width / 8)}
          fontWeight={600}
        >
          {icon ? `${icon} ` : ''}{name.length > width / 8 ? name.slice(0, Math.floor(width / 8)) + '...' : name}
        </text>
      )}
      {showAmount && (
        <text
          x={x + width / 2}
          y={y + height / 2 + 12}
          textAnchor="middle"
          dominantBaseline="central"
          fill="rgba(255,255,255,0.85)"
          fontSize={Math.min(12, width / 10)}
        >
          {formatCurrency(props.size)}
        </text>
      )}
    </g>
  );
}

export default function SpendingDistributionTreemap({
  aggregates,
  onCategoryClick,
  selectedCategoryId,
}: SpendingDistributionTreemapProps) {
  const theme = useTheme();

  const { treemapData, sortedList, total } = useMemo(() => {
    const parentAggs = aggregates.filter((a) => !a.parentCategoryId);
    const sorted = [...parentAggs].sort((a, b) => b.totalAmount - a.totalAmount);
    const total = sorted.reduce((s, a) => s + a.totalAmount, 0);

    const items: TreemapItem[] = sorted
      .filter((a) => a.totalAmount > 0)
      .map((a, i) => ({
        name: a.categoryName,
        icon: a.categoryIcon,
        categoryId: a.categoryId,
        size: a.totalAmount,
        color: CHART_COLORS[i % CHART_COLORS.length],
      }));

    return { treemapData: items, sortedList: sorted, total };
  }, [aggregates]);

  if (treemapData.length === 0) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', py: 6 }}>
        <Typography color="text.secondary">No spending data for this period.</Typography>
      </Box>
    );
  }

  return (
    <Box>
      <Box sx={{ width: '100%', height: { xs: 250, sm: 350 } }}>
        <ResponsiveContainer width="100%" height="100%">
          <Treemap
            data={treemapData}
            dataKey="size"
            nameKey="name"
            content={<CustomContent />}
            onClick={(node: any) => {
              if (onCategoryClick && node?.categoryId) {
                onCategoryClick(node.categoryId);
              }
            }}
          >
            <Tooltip
              formatter={(value) => formatCurrency(Number(value))}
              labelFormatter={(name) => {
                const label = String(name);
                const item = treemapData.find((d) => d.name === label);
                return item?.icon ? `${item.icon} ${label}` : label;
              }}
              contentStyle={{
                backgroundColor: theme.palette.background.paper,
                border: `1px solid ${theme.palette.divider}`,
                borderRadius: 8,
              }}
            />
          </Treemap>
        </ResponsiveContainer>
      </Box>

      {/* Sorted list */}
      <List dense sx={{ mt: 1 }}>
        {sortedList.map((agg, i) => {
          const pct = total > 0 ? ((agg.totalAmount / total) * 100).toFixed(1) : '0.0';
          return (
            <ListItemButton
              key={agg.categoryId}
              selected={selectedCategoryId === agg.categoryId}
              onClick={() => onCategoryClick?.(agg.categoryId)}
              sx={{ borderRadius: 1, py: 0.5 }}
            >
              <ListItemIcon sx={{ minWidth: 36 }}>
                <Box
                  sx={{
                    width: 14,
                    height: 14,
                    borderRadius: '3px',
                    backgroundColor: CHART_COLORS[i % CHART_COLORS.length],
                  }}
                />
              </ListItemIcon>
              <ListItemText
                primary={
                  <Typography variant="body2">
                    {agg.categoryIcon ? `${agg.categoryIcon} ` : ''}{agg.categoryName}
                  </Typography>
                }
              />
              <Typography variant="body2" sx={{ fontWeight: 500, mr: 2 }}>
                {formatCurrency(agg.totalAmount)}
              </Typography>
              <Typography variant="caption" color="text.secondary" sx={{ minWidth: 45, textAlign: 'right' }}>
                {pct}%
              </Typography>
            </ListItemButton>
          );
        })}
      </List>
    </Box>
  );
}
