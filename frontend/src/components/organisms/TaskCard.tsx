import React from 'react';
import { Button } from '../atoms/Button';
import { Target, Trophy, CheckCircle2 } from 'lucide-react';

export interface Task {
  id: number;
  title: string;
  description: string;
  pointsReward: number;
  targetSteps: number;
}

interface TaskCardProps {
  task: Task;
  isJoined: boolean;
  isJoining: boolean;
  onJoin: (taskId: number) => void;
}

export const TaskCard: React.FC<TaskCardProps> = ({
  task,
  isJoined,
  isJoining,
  onJoin,
}) => {
  return (
    <div className="glass-panel rounded-xl p-5 flex flex-col justify-between transition-all duration-300 border border-white/5 hover:border-white/10 relative overflow-hidden">
      <div className={`absolute top-0 left-0 right-0 h-1 bg-gradient-to-r ${isJoined ? 'from-accent-emerald to-teal-400' : 'from-accent-purple to-pink-500'}`} />

      <div>
        <div className="flex items-start justify-between gap-4 mb-3">
          <h4 className="text-base font-bold text-white tracking-tight leading-snug">
            {task.title}
          </h4>
          <span className="text-xs font-bold text-primary flex items-center gap-1 shrink-0">
            <Trophy className="w-3.5 h-3.5 text-accent-purple" />
            +{task.pointsReward} XP
          </span>
        </div>

        <p className="text-sm text-dark-muted mb-4 line-clamp-2 leading-relaxed">
          {task.description}
        </p>

        <div className="flex items-center gap-2 text-xs text-dark-muted mb-4">
          <Target className="w-3.5 h-3.5 text-accent-purple" />
          <span>Target: {task.targetSteps.toLocaleString()} steps</span>
        </div>
      </div>

      <div className="flex justify-end pt-3 border-t border-white/5">
        {isJoined ? (
          <span className="flex items-center gap-1 text-accent-emerald font-semibold text-xs px-3 py-1.5 bg-accent-emerald/10 border border-accent-emerald/20 rounded-lg">
            <CheckCircle2 className="w-4 h-4" /> Enrolled
          </span>
        ) : (
          <Button
            size="sm"
            variant="glass"
            disabled={isJoining}
            onClick={() => onJoin(task.id)}
          >
            {isJoining ? 'Enrolling...' : 'Enroll Task'}
          </Button>
        )}
      </div>
    </div>
  );
};
