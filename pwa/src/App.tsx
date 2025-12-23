import React from 'react';
import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { useStore } from './store/useStore';
import { Layout } from './components/Layout';
import { AuthPage } from './pages/Auth';
import { DocenteDashboard } from './pages/DocenteDashboard';
import { AlumnoDashboard } from './pages/AlumnoDashboard';
import { SubjectDetail } from './pages/SubjectDetail';
import { ClassDetail } from './pages/ClassDetail';
import { AboutPage } from './pages/AboutPage';

// Protected Route Wrapper
const ProtectedRoute = ({ children, allowedRoles }: { children: React.ReactNode, allowedRoles?: string[] }) => {
  const { user } = useStore();
  const location = useLocation();

  if (!user) {
    return <Navigate to="/auth" state={{ from: location }} replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
};

const Dashboard = () => {
  const { user } = useStore();
  return user?.role === 'docente' ? <DocenteDashboard /> : <AlumnoDashboard />;
};

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/auth" element={
          <Layout>
            <AuthPage />
          </Layout>
        } />

        <Route path="/" element={
          <ProtectedRoute>
            <Layout>
              <Dashboard />
            </Layout>
          </ProtectedRoute>
        } />

        <Route path="/asignatura/:id" element={
          <ProtectedRoute>
            <Layout>
              <SubjectDetail />
            </Layout>
          </ProtectedRoute>
        } />

        <Route path="/clase/:id" element={
          <ProtectedRoute>
            <Layout>
              <ClassDetail />
            </Layout>
          </ProtectedRoute>
        } />

        <Route path="/about" element={
          <Layout>
            <AboutPage />
          </Layout>
        } />

      </Routes>
    </BrowserRouter>
  );
}

export default App;
