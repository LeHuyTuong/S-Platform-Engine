import React from 'react';
import { AreaChart, Area, ResponsiveContainer, YAxis, XAxis, Tooltip } from 'recharts';

interface Props {
  history: (number | null)[];
}

export const SpeedChart: React.FC<Props> = ({ history }) => {
  const data = history.map((val, i) => ({ time: i, speed: val }));

  return (
    <div className="h-28 w-full bg-black/10 rounded-xl border border-border p-2">
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={data}>
          <defs>
            <linearGradient id="speedGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#8b5cf6" stopOpacity={0.3}/>
              <stop offset="95%" stopColor="#8b5cf6" stopOpacity={0}/>
            </linearGradient>
          </defs>
          <XAxis dataKey="time" hide />
          <YAxis hide domain={[0, 'dataMax + 5']} />
          <Tooltip 
            content={({ active, payload }) => {
              if (active && payload && payload.length) {
                return (
                  <div className="bg-slate-900 border border-border px-2 py-1 rounded text-[10px] font-bold">
                    {payload[0].value} MiB/s
                  </div>
                );
              }
              return null;
            }}
          />
          <Area
            type="monotone"
            dataKey="speed"
            stroke="#8b5cf6"
            strokeWidth={2}
            fillOpacity={1}
            fill="url(#speedGradient)"
            isAnimationActive={false}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
};
