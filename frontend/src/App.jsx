import { BrowserRouter, Routes, Route } from "react-router-dom";

import Navbar from "./components/Navbar";

import LoginPage from "./pages/LoginPage";
import DashboardPage from "./pages/DashboardPage";
import BooksPage from "./pages/BooksPage";
import LibrariesPage from "./pages/LibrariesPage";
import StockPage from "./pages/StockPage";
import TransfersPage from "./pages/TransfersPage";
import BorrowingPage from "./pages/BorrowingPage";
import MembersPage from "./pages/MembersPage";
import CategoriesPage from "./pages/CategoriesPage";
import BookDetailsPage from "./pages/BookDetailsPage";
import RegisterMemberPage from "./pages/RegisterMemberPage";
import MemberDetailsPage from "./pages/MemberDetailsPage.jsx";
import LoanDetailsPage from "./pages/LoanDetailsPage.jsx";
import BookAvailabilityPage from "./pages/BookAvailabilityPage.jsx";
function App() {
  return (
    <BrowserRouter>
      <Navbar />

      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/books" element={<BooksPage />} />
        <Route path="/libraries" element={<LibrariesPage />} />
        <Route path="/stock" element={<StockPage />} />
        <Route path="/transfers" element={<TransfersPage />} />
        <Route path="/borrowing" element={<BorrowingPage />} />
        <Route path="/members" element={<MembersPage />} />
        <Route path="/categories" element={<CategoriesPage />} />
        <Route path="/books/:id/availability" element={<BookAvailabilityPage />} />
        <Route path="/books/:id" element={<BookDetailsPage />} />
        <Route path="/members/register" element={<RegisterMemberPage />} />
        <Route path="/members/:id" element={<MemberDetailsPage />} />
        <Route path="/loans/:id" element={<LoanDetailsPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;

