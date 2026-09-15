import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import keycloak from "../keycloak";
import styles from "../styles/DashboardPage.module.css";

const API = "http://localhost:8000";

function DashboardPage() {
    const navigate = useNavigate();

    const [stats, setStats] = useState({
        books: 0,
        members: 0,
        libraries: 0,
        loans: 0,
    });

    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const loadDashboard = async () => {
            try {
                const headers = {
                    Authorization: `Bearer ${keycloak.token}`,
                };

                const [
                    booksResponse,
                    membersResponse,
                    librariesResponse,
                    loansResponse,
                ] = await Promise.all([
                    fetch(`${API}/api/books/available`, {
                        headers,
                    }),
                    fetch(`${API}/api/members/all`, {
                        headers,
                    }),
                    fetch(`${API}/api/libraries/available`, {
                        headers,
                    }),
                    fetch(`${API}/api/loans/all`, {
                        headers,
                    }),
                ]);

                const books = booksResponse.ok
                    ? await booksResponse.json()
                    : [];

                const members = membersResponse.ok
                    ? await membersResponse.json()
                    : [];

                const libraries = librariesResponse.ok
                    ? await librariesResponse.json()
                    : [];

                const loans = loansResponse.ok
                    ? await loansResponse.json()
                    : [];

                setStats({
                    books: books.length,
                    members: members.length,
                    libraries: libraries.length,
                    loans: loans.length,
                });
            } catch (error) {
                console.error(
                    "Could not load dashboard:",
                    error
                );
            } finally {
                setLoading(false);
            }
        };

        loadDashboard();
    }, []);

    const quickActions = [
        {
            icon: "📚",
            title: "Manage Books",
            description: "Browse and manage the catalogue",
            path: "/books",
        },
        {
            icon: "👤",
            title: "Register Member",
            description: "Add a new library member",
            path: "/members/register",
        },
        {
            icon: "🏛️",
            title: "Manage Libraries",
            description: "View and manage library branches",
            path: "/libraries",
        },
        {
            icon: "📦",
            title: "Manage Stock",
            description: "View books available at each library",
            path: "/stock",
        },
    ];

    return (
        <div className={styles.page}>
            <div className={styles.container}>
                {/* HEADER */}
                <section className={styles.hero}>
                    <div>
                        <p className={styles.eyebrow}>
                            LIBRARY SYSTEM
                        </p>

                        <h1 className={styles.title}>
                            Welcome back
                        </h1>

                        <p className={styles.subtitle}>
                            Here's an overview of your library
                            system.
                        </p>
                    </div>

                    <div className={styles.heroIcon}>
                        📚
                    </div>
                </section>

                {/* STATS */}
                <section className={styles.statsGrid}>
                    <div
                        className={styles.statCard}
                        onClick={() => navigate("/books")}
                    >
                        <div className={styles.statIcon}>
                            📚
                        </div>

                        <div>
                            <p className={styles.statLabel}>
                                Books
                            </p>

                            <p className={styles.statValue}>
                                {loading ? "—" : stats.books}
                            </p>

                            <p className={styles.statLink}>
                                View catalogue →
                            </p>
                        </div>
                    </div>

                    <div
                        className={styles.statCard}
                        onClick={() => navigate("/members")}
                    >
                        <div className={styles.statIcon}>
                            👥
                        </div>

                        <div>
                            <p className={styles.statLabel}>
                                Members
                            </p>

                            <p className={styles.statValue}>
                                {loading ? "—" : stats.members}
                            </p>

                            <p className={styles.statLink}>
                                View members →
                            </p>
                        </div>
                    </div>

                    <div
                        className={styles.statCard}
                        onClick={() => navigate("/libraries")}
                    >
                        <div className={styles.statIcon}>
                            🏛️
                        </div>

                        <div>
                            <p className={styles.statLabel}>
                                Libraries
                            </p>

                            <p className={styles.statValue}>
                                {loading
                                    ? "—"
                                    : stats.libraries}
                            </p>

                            <p className={styles.statLink}>
                                View libraries →
                            </p>
                        </div>
                    </div>

                    <div
                        className={styles.statCard}
                        onClick={() => navigate("/borrowing")}
                    >
                        <div className={styles.statIcon}>
                            📖
                        </div>

                        <div>
                            <p className={styles.statLabel}>
                                Loans
                            </p>

                            <p className={styles.statValue}>
                                {loading ? "—" : stats.loans}
                            </p>

                            <p className={styles.statLink}>
                                View loans →
                            </p>
                        </div>
                    </div>
                </section>

                {/* MAIN CONTENT */}
                <div className={styles.contentGrid}>
                    {/* QUICK ACTIONS */}
                    <section className={styles.section}>
                        <div className={styles.sectionHeader}>
                            <div>
                                <h2>Quick actions</h2>
                                <p>
                                    Common tasks and shortcuts
                                </p>
                            </div>
                        </div>

                        <div className={styles.actionsGrid}>
                            {quickActions.map((action) => (
                                <button
                                    key={action.path}
                                    className={styles.actionCard}
                                    onClick={() =>
                                        navigate(action.path)
                                    }
                                >
                                    <div
                                        className={
                                            styles.actionIcon
                                        }
                                    >
                                        {action.icon}
                                    </div>

                                    <div
                                        className={
                                            styles.actionContent
                                        }
                                    >
                                        <h3>
                                            {action.title}
                                        </h3>

                                        <p>
                                            {action.description}
                                        </p>
                                    </div>

                                    <span
                                        className={
                                            styles.actionArrow
                                        }
                                    >
                                        →
                                    </span>
                                </button>
                            ))}
                        </div>
                    </section>

                    {/* SYSTEM OVERVIEW */}
                    <section
                        className={`${styles.section} ${styles.overviewSection}`}
                    >
                        <div className={styles.sectionHeader}>
                            <div>
                                <h2>System overview</h2>
                                <p>
                                    Manage your library system
                                </p>
                            </div>
                        </div>

                        <div className={styles.overviewList}>
                            <button
                                onClick={() =>
                                    navigate("/transfers")
                                }
                                className={styles.overviewItem}
                            >
                                <span
                                    className={
                                        styles.overviewIcon
                                    }
                                >
                                    🔄
                                </span>

                                <span>
                                    <strong>
                                        Transfers
                                    </strong>
                                    <small>
                                        Move stock between
                                        libraries
                                    </small>
                                </span>

                                <span>→</span>
                            </button>

                            <button
                                onClick={() =>
                                    navigate("/borrowing")
                                }
                                className={styles.overviewItem}
                            >
                                <span
                                    className={
                                        styles.overviewIcon
                                    }
                                >
                                    📖
                                </span>

                                <span>
                                    <strong>
                                        Borrowing
                                    </strong>
                                    <small>
                                        Manage active and
                                        completed loans
                                    </small>
                                </span>

                                <span>→</span>
                            </button>

                            <button
                                onClick={() =>
                                    navigate("/categories")
                                }
                                className={styles.overviewItem}
                            >
                                <span
                                    className={
                                        styles.overviewIcon
                                    }
                                >
                                    🗂️
                                </span>

                                <span>
                                    <strong>
                                        Categories
                                    </strong>
                                    <small>
                                        Organize the book
                                        catalogue
                                    </small>
                                </span>

                                <span>→</span>
                            </button>

                            <button
                                onClick={() =>
                                    navigate("/stock")
                                }
                                className={styles.overviewItem}
                            >
                                <span
                                    className={
                                        styles.overviewIcon
                                    }
                                >
                                    📦
                                </span>

                                <span>
                                    <strong>
                                        Inventory
                                    </strong>
                                    <small>
                                        Monitor stock across
                                        libraries
                                    </small>
                                </span>

                                <span>→</span>
                            </button>
                        </div>
                    </section>
                </div>

                {/* BOTTOM WELCOME CARD */}
                <section className={styles.bottomCard}>
                    <div>
                        <p className={styles.bottomEyebrow}>
                            LIBRARY MANAGEMENT
                        </p>

                        <h2>
                            Everything you need in one place.
                        </h2>

                        <p>
                            Manage books, members, inventory,
                            borrowing and library transfers
                            from the dashboard.
                        </p>
                    </div>

                    <button
                        onClick={() => navigate("/books")}
                        className={styles.primaryButton}
                    >
                        Browse Books →
                    </button>
                </section>
            </div>
        </div>
    );
}

export default DashboardPage;