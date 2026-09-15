import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import keycloak from "../keycloak";
import styles from "../styles/TransfersPage.module.css";

const API = "http://localhost:8000";

const getId = (value) => {
    if (!value) return null;

    if (typeof value === "string") {
        return value;
    }

    return value.value ?? value.id ?? null;
};

const getNextAction = (status) => {
    switch ((status ?? "").toUpperCase()) {
        case "ACCEPTED":
            return { path: "ship", label: "Mark Shipped" };
        case "SHIPPED":
            return { path: "complete", label: "Mark Completed" };
        default:
            return null;
    }
};

function TransfersPage() {
    const [transfers, setTransfers] = useState([]);
    const [libraries, setLibraries] = useState({});
    const [books, setBooks] = useState({});
    const [loading, setLoading] = useState(true);
    const [savingTransferId, setSavingTransferId] = useState("");
    const [sourceLibraryFilter, setSourceLibraryFilter] = useState("");
    const [destinationLibraryFilter, setDestinationLibraryFilter] = useState("");
    const [error, setError] = useState("");

    const getStatusClass = (status) => {
        switch ((status ?? "").toLowerCase()) {
            case "requested":
            case "pending":
                return styles.statusPending;
            case "accepted":
            case "approved":
                return styles.statusApproved;
            case "shipped":
                return styles.statusShipped;
            case "completed":
                return styles.statusCompleted;
            case "rejected":
                return styles.statusRejected;
            default:
                return styles.statusDefault;
        }
    };

    const loadData = useCallback(async () => {
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

            const libraryMap = {};

            librariesData.forEach((library) => {
                const id = getId(library.id);

                if (id) {
                    libraryMap[id] = library;
                }
            });

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
            setError("Could not load transfers.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        // eslint-disable-next-line react-hooks/set-state-in-effect
        loadData();
    }, [loadData]);

    const libraryOptions = useMemo(() => {
        return Object.entries(libraries)
            .map(([id, library]) => ({
                id,
                name: library.name ?? id,
            }))
            .sort((a, b) => a.name.localeCompare(b.name));
    }, [libraries]);

    const filteredTransfers = useMemo(() => {
        return transfers.filter((transfer) => {
            const sourceLibraryId = getId(transfer.sourceLibraryId);
            const destinationLibraryId = getId(transfer.destinationLibraryId);

            return (
                (!sourceLibraryFilter || sourceLibraryId === sourceLibraryFilter) &&
                (!destinationLibraryFilter ||
                    destinationLibraryId === destinationLibraryFilter)
            );
        });
    }, [destinationLibraryFilter, sourceLibraryFilter, transfers]);

    const handleTransferAction = async (transfer, action) => {
        const selectedAction = action ?? getNextAction(transfer.status);

        if (!selectedAction) {
            return;
        }

        setSavingTransferId(transfer.id);
        setError("");

        try {
            const body = selectedAction.path === "complete"
                ? { id: transfer.id, completedAt: new Date().toISOString() }
                : selectedAction.path === "cancel"
                    ? {
                        id: transfer.id,
                        reason: "Cancelled from transfers page",
                    }
                    : { id: transfer.id };

            const response = await fetch(`${API}/api/transfers/${selectedAction.path}`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${keycloak.token}`,
                },
                body: JSON.stringify(body),
            });

            if (!response.ok) {
                const message = await response.text();
                throw new Error(
                    message || `Failed to ${selectedAction.path} transfer.`
                );
            }

            await loadData();
        } catch (error) {
            console.error(error);
            setError(error.message || "Could not update transfer.");
        } finally {
            setSavingTransferId("");
        }
    };

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
                <h1 className={styles.title}>Transfer Requests</h1>

                <p className={styles.subtitle}>
                    Request copies from a library through that book's stock row.
                </p>
            </div>

            {error && <div className={styles.error}>{error}</div>}

            <div className={styles.filters}>
                <label className={styles.filterField}>
                    <span>Supplying Library</span>
                    <select
                        value={sourceLibraryFilter}
                        onChange={(event) =>
                            setSourceLibraryFilter(event.target.value)
                        }
                    >
                        <option value="">All supplying libraries</option>
                        {libraryOptions.map((library) => (
                            <option key={library.id} value={library.id}>
                                {library.name}
                            </option>
                        ))}
                    </select>
                </label>

                <label className={styles.filterField}>
                    <span>Requesting Library</span>
                    <select
                        value={destinationLibraryFilter}
                        onChange={(event) =>
                            setDestinationLibraryFilter(event.target.value)
                        }
                    >
                        <option value="">All requesting libraries</option>
                        {libraryOptions.map((library) => (
                            <option key={library.id} value={library.id}>
                                {library.name}
                            </option>
                        ))}
                    </select>
                </label>
            </div>

            {transfers.length === 0 ? (
                <div className={styles.emptyState}>
                    <h2>No book transfers found</h2>
                    <p>
                        When you request a book from another library, it'll show up here.
                    </p>
                </div>
            ) : filteredTransfers.length === 0 ? (
                <div className={styles.emptyState}>
                    <h2>No matching book transfers</h2>
                    <p>Try changing the library filters.</p>
                </div>
            ) : (
                <div className={styles.grid}>
                    {filteredTransfers.map((transfer, index) => {
                        const sourceLibraryId = getId(transfer.sourceLibraryId);
                        const destinationLibraryId = getId(
                            transfer.destinationLibraryId
                        );

                        const bookId = getId(transfer.titleId);

                        const sourceLibrary = libraries[sourceLibraryId];
                        const destinationLibrary = libraries[destinationLibraryId];
                        const book = books[bookId];
                        const nextAction = getNextAction(transfer.status);
                        const canCancel = ["REQUESTED", "ACCEPTED"].includes(
                            (transfer.status ?? "").toUpperCase()
                        );

                        return (
                            <div key={transfer.id ?? index} className={styles.card}>
                                <div className={styles.cardHeader}>
                                    <h2 className={styles.cardTitle}>
                                        Transfer Request
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

                                {(canCancel || nextAction) && (
                                    <div className={styles.actions}>
                                        <div>
                                            {canCancel && (
                                                <button
                                                    className={styles.dangerButton}
                                                    onClick={() =>
                                                        handleTransferAction(transfer, {
                                                            path: "cancel",
                                                        })
                                                    }
                                                    disabled={savingTransferId === transfer.id}
                                                >
                                                    {savingTransferId === transfer.id
                                                        ? "Updating..."
                                                        : "Cancel"}
                                                </button>
                                            )}
                                        </div>

                                        <div>
                                            {nextAction && (
                                                <button
                                                    className={styles.primaryButton}
                                                    onClick={() =>
                                                        handleTransferAction(
                                                            transfer,
                                                            nextAction
                                                        )
                                                    }
                                                    disabled={savingTransferId === transfer.id}
                                                >
                                                    {savingTransferId === transfer.id
                                                        ? "Updating..."
                                                        : nextAction.label}
                                                </button>
                                            )}
                                        </div>
                                    </div>
                                )}
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}

export default TransfersPage;
