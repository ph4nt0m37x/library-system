import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import keycloak from "../keycloak";
import styles from "../styles/BookAvailabilityPage.module.css";

const API = "http://localhost:8000";

const getId = (value) => {
    if (!value) return "";
    if (typeof value === "string") return value;
    return value.value ?? value.id ?? "";
};

function BookAvailabilityPage() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [book, setBook] = useState(null);
    const [availability, setAvailability] = useState([]);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const token = keycloak.token;

    useEffect(() => {
        const loadAvailability = async () => {
            try {
                const headers = {
                    Authorization: `Bearer ${token}`,
                };

                const [bookResponse, librariesResponse, stockResponse] =
                    await Promise.all([
                        fetch(
                            `${API}/api/books/${encodeURIComponent(id)}`,
                            { headers }
                        ),
                        fetch(`${API}/api/libraries/available`, {
                            headers,
                        }),
                        fetch(
                            `${API}/api/stock/book/${encodeURIComponent(id)}`,
                            { headers }
                        ),
                    ]);

                if (!bookResponse.ok) {
                    throw new Error("Book not found.");
                }

                if (!librariesResponse.ok || !stockResponse.ok) {
                    throw new Error(
                        "Could not load book availability."
                    );
                }

                const [bookData, libraries, stock] =
                    await Promise.all([
                        bookResponse.json(),
                        librariesResponse.json(),
                        stockResponse.json(),
                    ]);

                const libraryNames = new Map(
                    libraries.map((library) => [
                        String(getId(library.id)),
                        library.name,
                    ])
                );

                setBook(bookData);

                setAvailability(
                    stock.map((item) => ({
                        ...item,
                        libraryName:
                            libraryNames.get(
                                String(getId(item.libraryId))
                            ) ?? getId(item.libraryId),
                    }))
                );
            } catch (err) {
                setError(
                    err.message ||
                    "Could not load book availability."
                );
            } finally {
                setLoading(false);
            }
        };

        loadAvailability();
    }, [id, token]);

    if (loading) {
        return (
            <div className={styles.page}>
                <div className={styles.loadingWrap}>
                    Loading availability...
                </div>
            </div>
        );
    }

    const totalCopies = availability.reduce(
        (sum, item) => sum + (item.totalQuantity || 0),
        0
    );

    const totalAvailable = availability.reduce(
        (sum, item) => sum + (item.availableQuantity || 0),
        0
    );

    const totalBorrowed = availability.reduce(
        (sum, item) => sum + (item.borrowedQuantity || 0),
        0
    );

    return (
        <div className={styles.page}>
            <div className={styles.headerRow}>
                <button
                    type="button"
                    className={styles.backButton}
                    onClick={() => navigate(-1)}
                >
                    ← Back
                </button>

                <div className={styles.pageHeader}>
                    <h1 className={styles.pageTitle}>
                        {book?.title || "Book Availability"}
                    </h1>

                    <p className={styles.pageSubtitle}>
                        Availability by library
                    </p>
                </div>
            </div>

            {error && (
                <div className={styles.error} role="alert">
                    {error}
                </div>
            )}

            {!error && (
                <>
                    {availability.length > 0 && (
                        <section className={styles.summaryCard}>
                            <div className={styles.summaryItem}>
                                <span className={styles.summaryLabel}>
                                    Libraries
                                </span>

                                <span className={styles.summaryValue}>
                                    {availability.length}
                                </span>
                            </div>

                            <div className={styles.summaryItem}>
                                <span className={styles.summaryLabel}>
                                    Total copies
                                </span>

                                <span className={styles.summaryValue}>
                                    {totalCopies}
                                </span>
                            </div>

                            <div className={styles.summaryItem}>
                                <span className={styles.summaryLabel}>
                                    Available
                                </span>

                                <span
                                    className={`${styles.summaryValue} ${styles.summaryAvailable}`}
                                >
                                    {totalAvailable}
                                </span>
                            </div>

                            <div className={styles.summaryItem}>
                                <span className={styles.summaryLabel}>
                                    Borrowed
                                </span>

                                <span
                                    className={`${styles.summaryValue} ${styles.summaryBorrowed}`}
                                >
                                    {totalBorrowed}
                                </span>
                            </div>
                        </section>
                    )}

                    <section className={styles.section}>
                        <div className={styles.sectionHeader}>
                            <h2 className={styles.sectionTitle}>
                                Stock per library

                                <span className={styles.sectionCount}>
                                    ({availability.length})
                                </span>
                            </h2>
                        </div>

                        {availability.length === 0 ? (
                            <div className={styles.emptyState}>
                                This book is not currently stocked in
                                any library.
                            </div>
                        ) : (
                            <div className={styles.list}>
                                {availability.map((item) => {
                                    const available =
                                        item.availableQuantity || 0;

                                    const total =
                                        item.totalQuantity || 0;

                                    const libraryId = getId(
                                        item.libraryId
                                    );

                                    const statusClass =
                                        available === 0
                                            ? styles.statusUnavailable
                                            : available < total
                                                ? styles.statusPartial
                                                : styles.statusAvailable;

                                    const statusLabel =
                                        available === 0
                                            ? "Unavailable"
                                            : available < total
                                                ? "Limited"
                                                : "Available";

                                    return (
                                        <div
                                            key={libraryId}
                                            className={styles.listItem}
                                        >
                                            <div
                                                className={
                                                    styles.listItemHeader
                                                }
                                            >
                                                <h3
                                                    className={
                                                        styles.listItemTitle
                                                    }
                                                >
                                                    <Link
                                                        to={`/stock?libraryId=${encodeURIComponent(
                                                            libraryId
                                                        )}`}
                                                        className={
                                                            styles.libraryLink
                                                        }
                                                    >
                                                        {item.libraryName}
                                                    </Link>
                                                </h3>

                                                <span
                                                    className={`${styles.status} ${statusClass}`}
                                                >
                                                    {statusLabel}
                                                </span>
                                            </div>

                                            <div
                                                className={
                                                    styles.infoGrid
                                                }
                                            >
                                                <div
                                                    className={
                                                        styles.infoItem
                                                    }
                                                >
                                                    <span
                                                        className={
                                                            styles.infoLabel
                                                        }
                                                    >
                                                        Total copies
                                                    </span>

                                                    <span
                                                        className={
                                                            styles.infoValue
                                                        }
                                                    >
                                                        {total}
                                                    </span>
                                                </div>

                                                <div
                                                    className={
                                                        styles.infoItem
                                                    }
                                                >
                                                    <span
                                                        className={
                                                            styles.infoLabel
                                                        }
                                                    >
                                                        Available
                                                    </span>

                                                    <span
                                                        className={`${styles.infoValue} ${styles.infoValueAvailable}`}
                                                    >
                                                        {available}
                                                    </span>
                                                </div>

                                                <div
                                                    className={
                                                        styles.infoItem
                                                    }
                                                >
                                                    <span
                                                        className={
                                                            styles.infoLabel
                                                        }
                                                    >
                                                        Borrowed
                                                    </span>

                                                    <span
                                                        className={
                                                            styles.infoValue
                                                        }
                                                    >
                                                        {item.borrowedQuantity ||
                                                            0}
                                                    </span>
                                                </div>
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        )}
                    </section>
                </>
            )}
        </div>
    );
}

export default BookAvailabilityPage;