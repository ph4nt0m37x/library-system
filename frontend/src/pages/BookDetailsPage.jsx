import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import styles from "../styles/BookDetailsPage.module.css";

const API = "http://localhost:8000";

function BookDetailsPage() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [book, setBook] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const fetchBook = async () => {
            try {
                const token = localStorage.getItem("token");

                const response = await fetch(`${API}/api/books/${id}`, {
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                });

                if (!response.ok) {
                    throw new Error("Book not found");
                }

                const data = await response.json();
                setBook(data);
            } catch (err) {
                setError(err.message);
            } finally {
                setLoading(false);
            }
        };

        fetchBook();
    }, [id]);

    if (loading) {
        return (
            <div className={styles.page}>
                <div className={styles.stateWrap}>
                    <p className={styles.stateText}>Loading book...</p>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className={styles.page}>
                <div className={styles.stateWrap}>
                    <h2 className={styles.stateTitle}>{error}</h2>
                    <p className={styles.stateText}>
                        The book you're looking for may have been removed or
                        the link is incorrect.
                    </p>
                    <button
                        className={styles.stateButton}
                        onClick={() => navigate(-1)}
                    >
                        ← Go back
                    </button>
                </div>
            </div>
        );
    }

    if (!book) {
        return (
            <div className={styles.page}>
                <div className={styles.stateWrap}>
                    <h2 className={styles.stateTitle}>Book not found</h2>
                    <button
                        className={styles.stateButton}
                        onClick={() => navigate(-1)}
                    >
                        ← Go back
                    </button>
                </div>
            </div>
        );
    }

    const categoryName =
        typeof book.category === "object"
            ? book.category?.name
            : book.category || "No category";

    return (
        <div className={styles.page}>
            <button
                className={styles.backButton}
                onClick={() => navigate(-1)}
            >
                ← Back
            </button>

            <div className={styles.card}>
                <span className={styles.categoryBadge}>
                    {categoryName}
                </span>

                <h1 className={styles.title}>{book.title}</h1>

                <p className={styles.author}>by {book.author}</p>

                <div className={styles.metaGrid}>
                    <div className={styles.metaItem}>
                        <span className={styles.metaLabel}>ISBN</span>
                        <span
                            className={`${styles.metaValue} ${styles.metaValueMono}`}
                        >
                            {book.isbn || "—"}
                        </span>
                    </div>

                    <div className={styles.metaItem}>
                        <span className={styles.metaLabel}>
                            Publication Year
                        </span>
                        <span className={styles.metaValue}>
                            {book.publicationYear || "—"}
                        </span>
                    </div>
                </div>

                <h2 className={styles.sectionTitle}>Description</h2>

                {book.description ? (
                    <p className={styles.description}>
                        {book.description}
                    </p>
                ) : (
                    <p
                        className={`${styles.description} ${styles.descriptionMuted}`}
                    >
                        No description available.
                    </p>
                )}
            </div>
        </div>
    );
}

export default BookDetailsPage;