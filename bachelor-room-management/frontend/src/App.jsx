import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { RoomProvider } from "./context/RoomContext";
import ProtectedRoute from "./routes/ProtectedRoute";
import AppLayout from "./components/AppLayout";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import DashboardPage from "./pages/DashboardPage";
import AdminPanelPage from "./pages/AdminPanelPage";
import MemberPanelPage from "./pages/MemberPanelPage";
import BoardPage from "./pages/BoardPage";
import RoomSetupPage from "./pages/RoomSetupPage";
import JoinByLinkPage from "./pages/JoinByLinkPage";

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          <Route
            element={
              <ProtectedRoute>
                <RoomProvider>
                  <AppLayout />
                </RoomProvider>
              </ProtectedRoute>
            }
          >
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/admin" element={<AdminPanelPage />} />
            <Route path="/member" element={<MemberPanelPage />} />
            <Route path="/board" element={<BoardPage />} />
            <Route path="/rooms/new" element={<RoomSetupPage />} />
            <Route path="/join/:code" element={<JoinByLinkPage />} />
          </Route>

          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
