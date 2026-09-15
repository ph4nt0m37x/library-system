import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import keycloak from "../keycloak";
import styles from "../styles/TransfersPage.module.css";

const API = "http://localhost:8000";

function TransfersPage() {
    const [transfers, setTransfers] = useState([]);
    const [libraries, setLibraries] = useState({});
    const [books, setBooks] = useState({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const getId = (value) => {
        if (!value) return null;

        if (typeof value === "string") {
            return value;
        }

        return value.value ?? value.id ?? null;
    };

    const getStatusClass = (status) => {
        switch ((status ?? "").toLowerCase()) {
            case "pending":
                return styles.statusPending;
            case "approved":
                return styles.statusApproved;
            case "completed":
                return styles.statusCompleted;
            case "rejected":
                return styles.statusRejected;
            default:
                return styles.statusDefault;
        }
    };

    useEffect(() => {
        const loadData = async () => {
            try {
                const headers = {
                    Authorization: `Bearer ${keycloak.token}`,
                };

                const [transfersResponse, librariesResponse, booksResponse] =
                    await Promise.all([
                        fetch(`${API}/api/transfers`, { headers }),
                        fetch(`${API}/api/libraries/available`, { headers }),
                        fetch(`${API}/api/books/available`, { headers }),
                    ]);

                if (!transfersResponse.ok) {
                    throw new Error(
                        `Failed to fetch transfers: ${transfersResponse.status}`
                    );
                }

                if (!librariesResponse.ok) {
                    throw new Error(
                        `Failed to fetch libraries: ${librariesResponse.status}`
                    );
                }

                if (!booksResponse.ok) {
                    throw new Error(
                        `Failed to fetch books: ${booksResponse.status}`
                    );
                }

                const transfersData = await transfersResponse.json();
                const librariesData = await librariesResponse.json();
                const booksData = await booksResponse.json();

                // Map library ID -> library
                const libraryMap = {};

                librariesData.forEach((library) => {
                    const id = getId(library.id);

                    if (id) {
                        libraryMap[id] = library;
                    }
                });

                // Map book ID -> book
                const bookMap = {};

                booksData.forEach((book) => {
                    const id = getId(book.id);

                    if (id) {
                        bookMap[id] = book;
                    }
                });

                setTransfers(transfersData);
                setLibraries(libraryMap);
                setBooks(bookMap);
            } catch (error) {
                console.error(error);
                setError("Could not load book requests.");
            } finally {
                setLoading(false);
            }
        };

        loadData();
    }, []);

    if (loading) {
        return (
            <div className={styles.page}>
                <div className={styles.emptyState}>
                    <h2>Loading transfers…</h2>
                </div>
            </div>
        );
    }

    return (
        <div className={styles.page}>
            <div className={styles.header}>
                <h1 className={styles.title}>Book Requests</h1>

                <p className={styles.subtitle}>
                    Request copies from a library through that book's stock row.
                </p>
            </div>

            {error && <div className={styles.error}>{error}</div>}

            {transfers.length === 0 ? (
                <div className={styles.emptyState}>
                    <h2>No book requests found</h2>
                    <p>
                        When you request a book from another library, it'll show up here.
                    </p>
                </div>
            ) : (
                <div className={styles.grid}>
                    {transfers.map((transfer, index) => {
                        const sourceLibraryId = getId(transfer.sourceLibraryId);
                        const destinationLibraryId = getId(
                            transfer.destinationLibraryId
                        );

                        const bookId = getId(transfer.titleId);

                        const sourceLibrary = libraries[sourceLibraryId];
                        const destinationLibrary = libraries[destinationLibraryId];
                        const book = books[bookId];

                        return (
                            <div key={index} className={styles.card}>
                                <div className={styles.cardHeader}>
                                    <h2 className={styles.cardTitle}>
                                        Book Request
                                    </h2>

                                    <span
                                        className={`${styles.status} ${getStatusClass(
                                            transfer.status
                                        )}`}
                                    >
                    {transfer.status ?? "unknown"}
                  </span>
                                </div>

                                <div className={styles.details}>
                                    {/* Book — full width */}
                                    <div className={styles.detailRow}>
                    <span className={styles.detailLabel}>
                      Book
                    </span>

                                        {book ? (
                                            <Link
                                                to={`/books/${bookId}`}
                                                className={styles.bookLink}
                                            >
                                                {book.title}
                                            </Link>
                                        ) : (
                                            <span className={styles.detailValue}>
                        {transfer.title ?? "Unknown book"}
                      </span>
                                        )}
                                    </div>

                                    {/* Supplying + Requesting side-by-side */}
                                    <div className={styles.libraryRow}>
                                        <div className={styles.detailRow}>
                      <span className={styles.detailLabel}>
                        Supplying Library
                      </span>

                                            <span className={styles.detailValue}>
                        {sourceLibrary?.name ?? "Unknown library"}
                      </span>
                                        </div>

                                        <div className={styles.detailRow}>
                      <span className={styles.detailLabel}>
                        Requesting Library
                      </span>

                                            <span className={styles.detailValue}>
                        {destinationLibrary?.name ?? "Unknown library"}
                      </span>
                                        </div>
                                    </div>

                                    {/* Requested By — pinned bottom-left */}
                                    <div className={styles.requestedBy}>
                    <span className={styles.detailLabel}>
                      Requested By
                    </span>

                                        <span className={styles.detailValue}>
                      {transfer.requestedBy ?? "—"}
                    </span>
                                    </div>
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}

export default TransfersPage;