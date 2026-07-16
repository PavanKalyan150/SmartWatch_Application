import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { TaskCard } from '../organisms/TaskCard';
import type { Task } from '../organisms/TaskCard';
import { Spinner } from '../atoms/Spinner';
import { ListTodo } from 'lucide-react';
import api from '../../services/api';

export const TasksPage: React.FC = () => {
  const { user, refreshUser } = useAuth();
  const [tasks, setTasks] = useState<Task[]>([]);
  const [joinedTaskIds, setJoinedTaskIds] = useState<number[]>([]);
  const [loading, setLoading] = useState(true);
  const [joiningId, setJoiningId] = useState<number | null>(null);

  useEffect(() => {
    if (user) {
      fetchTasksAndRegistrations();
    }
  }, [user?.id]);

  const fetchTasksAndRegistrations = async () => {
    if (!user) return;
    try {
      setLoading(true);

      // 1. Fetch joined tasks
      const regResponse = await api.get(`/user/${user.id}/tasks`);
      const joinedIds = (regResponse.data || []).map((reg: any) => reg.task?.id);
      setJoinedTaskIds(joinedIds);

      // 2. Fetch all tasks
      const taskResponse = await api.get('/task');
      setTasks(taskResponse.data || []);
    } catch (err) {
      console.error('Error fetching tasks:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleJoin = async (taskId: number) => {
    if (!user) return;
    try {
      setJoiningId(taskId);
      await api.post(`/task/${taskId}/register`, null, {
        params: { userId: user.id },
      });
      await fetchTasksAndRegistrations();
      await refreshUser();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to enroll in task.');
    } finally {
      setJoiningId(null);
    }
  };

  return (
    <>
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight text-white flex items-center gap-2">
            <ListTodo className="w-6 h-6 text-accent-purple" />
            Daily & Weekly Goals
          </h1>
          <p className="text-sm text-dark-muted mt-1">
            Enroll in tasks, log step updates, and earn XP rewards instantly.
          </p>
        </div>
      </div>

      {loading ? (
        <div className="flex flex-col items-center justify-center py-24 gap-3">
          <Spinner size="lg" />
          <p className="text-xs text-dark-muted">Loading available tasks...</p>
        </div>
      ) : tasks.length === 0 ? (
        <div className="text-center py-20 glass-panel rounded-2xl border border-white/5">
          <ListTodo className="w-12 h-12 text-dark-muted mx-auto mb-3" />
          <p className="text-sm font-semibold text-white">No tasks available</p>
          <p className="text-xs text-dark-muted mt-1 max-w-xs mx-auto">
            Check back later for newly added smartwatch tasks and activities.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {tasks.map((task) => (
            <TaskCard
              key={task.id}
              task={task}
              isJoined={joinedTaskIds.includes(task.id)}
              isJoining={joiningId === task.id}
              onJoin={handleJoin}
            />
          ))}
        </div>
      )}
    </>
  );
};
