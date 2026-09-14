import { Link } from "react-router-dom";

function Navbar() {
    return (
        <nav>
            <Link to="/dashboard">Dashboard</Link>{" | "}
            <Link to="/books">Books</Link>{" | "}
            <Link to="/libraries">Libraries</Link>{" | "}
            <Link to="/stock">Stock</Link>{" | "}
            <Link to="/transfers">Transfers</Link>{" | "}
            <Link to="/borrowing">Borrowing</Link>{" | "}
            <Link to="/members">Members</Link>{" | "}
            <Link to="/categories">Categories</Link>{" | "}
            <Link to="/login">Logout</Link>
        </nav>
    );
}

export default Navbar;