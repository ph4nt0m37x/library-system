import { Link, NavLink } from "react-router-dom";
import { logout } from "../keycloak";
import styles from "../styles/Navbar.module.css";

function Navbar() {
    const navLinkClass = ({ isActive }) =>
        isActive
            ? `${styles.link} ${styles.linkActive}`
            : styles.link;

    return (
        <nav className={styles.navbar}>
            <Link to="/dashboard" className={styles.brand}>
                <span className={styles.brandMark}>L</span>
                <span>Library</span>
            </Link>

            <div className={styles.links}>

                <NavLink to="/books" className={navLinkClass}>
                    Book Catalog
                </NavLink>
                <NavLink to="/stock" className={navLinkClass}>
                    Library Stock
                </NavLink>
                <NavLink to="/transfers" className={navLinkClass}>
                    Transfers
                </NavLink>

                <NavLink to="/borrowing" className={navLinkClass}>
                    Loans
                </NavLink>

                <NavLink to="/members" className={navLinkClass}>
                    Members
                </NavLink>
            </div>

            <div className={styles.actions}>
                <button
                    type="button"
                    className={styles.logoutButton}
                    onClick={logout}
                >
                    Logout
                </button>
            </div>
        </nav>
    );
}

export default Navbar;