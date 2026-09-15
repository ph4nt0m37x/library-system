    import { useEffect, useMemo, useState } from "react";
    import { useNavigate, useSearchParams } from "react-router-dom";
    import styles from "../styles/StockPage.module.css";
    import keycloak from "../keycloak";

    const API = "http://localhost:8000";

    function StockPage() {
        const [searchParams] = useSearchParams();
        const navigate = useNavigate();
        const [libraries, setLibraries] = useState([]);
        const [books, setBooks] = useState([]);
        const [members, setMembers] = useState([]);
        const [pendingTransfers, setPendingTransfers] = useState([]);
        const [stock, setStock] = useState([]);

        const [selectedLibraryId, setSelectedLibraryId] = useState("");

        const [loadingLibraries, setLoadingLibraries] = useState(true);
        const [loadingBooks, setLoadingBooks] = useState(true);
        const [loadingStock, setLoadingStock] = useState(false);

        const [error, setError] = useState("");

        const [modal, setModal] = useState(null);
        // null
        // { type: "add", stock }
        // { type: "remove", stock }
        // { type: "new-stock" }

        const [quantity, setQuantity] = useState(1);
        const [saving, setSaving] = useState(false);

        const [bookSearch, setBookSearch] = useState("");
        const [selectedBook, setSelectedBook] = useState(null);
        const [memberSearch, setMemberSearch] = useState("");
        const [selectedMemberId, setSelectedMemberId] = useState("");
        const [loadingMembers, setLoadingMembers] = useState(false);
        const [loadingTransfers, setLoadingTransfers] = useState(false);
        const [sourceLibraryId, setSourceLibraryId] = useState("");
        const [sourceStock, setSourceStock] = useState([]);
        const [requestedBookId, setRequestedBookId] = useState("");
        const [loadingSourceStock, setLoadingSourceStock] = useState(false);

        const token = localStorage.getItem("token");

        const getId = (value) => {
            if (!value) return null;

            if (typeof value === "string") {
                return value;
            }

            return value.value ?? value.id ?? null;
        };

        const getBookId = (book) => {
            return getId(book.id);
        };

        const getStockBookId = (stockItem) => {
            return getId(stockItem.bookId);
        };

        const getLibraryId = (library) => {
            return getId(library.id);
        };

        const fetchLibraries = async () => {
            setLoadingLibraries(true);

            try {
                const response = await fetch(`${API}/api/libraries/available`, {
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                });

                if (!response.ok) {
                    throw new Error(`Failed to load libraries: ${response.status}`);
                }

                const data = await response.json();

                setLibraries(data);

                if (data.length > 0) {
                    const requestedLibraryId = searchParams.get("libraryId");
                    const matchingLibrary = data.find(
                        (library) => getLibraryId(library) === requestedLibraryId
                    );

                    setSelectedLibraryId(
                        matchingLibrary
                            ? requestedLibraryId
                            : getLibraryId(data[0])
                    );
                }
            } catch (err) {
                setError(err.message);
            } finally {
                setLoadingLibraries(false);
            }
        };

        const fetchBooks = async () => {
            setLoadingBooks(true);

            try {
                const response = await fetch(`${API}/api/books/available`, {
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                });

                if (!response.ok) {
                    throw new Error(`Failed to load books: ${response.status}`);
                }

                const data = await response.json();
                setBooks(data);
            } catch (err) {
                setError(err.message);
            } finally {
                setLoadingBooks(false);
            }
        };

        const fetchStock = async (libraryId, showLoading = true) => {
            if (!libraryId) {
                setStock([]);
                return;
            }

            if (showLoading) {
                setLoadingStock(true);
            }
            setError("");

            try {
                const response = await fetch(
                    `${API}/api/stock/${encodeURIComponent(libraryId)}`,
                    {
                        headers: {
                            Authorization: `Bearer ${token}`,
                        },
                    }
                );

                if (!response.ok) {
                    throw new Error(`Failed to load stock: ${response.status}`);
                }

                const data = await response.json();
                setStock(data);
                return data;
            } catch (err) {
                setError(err.message);
                setStock([]);
            } finally {
                if (showLoading) {
                    setLoadingStock(false);
                }
            }
        };

        const fetchPendingTransfers = async (libraryId) => {
            if (!libraryId) {
                setPendingTransfers([]);
                return;
            }

            setLoadingTransfers(true);
            try {
                const query = new URLSearchParams({
                    status: "REQUESTED",
                    sourceLibraryId: libraryId,
                });
                const response = await fetch(`${API}/api/transfers?${query}`, {
                    headers: { Authorization: `Bearer ${token}` },
                });
                if (!response.ok) {
                    throw new Error(`Failed to load pending transfers: ${response.status}`);
                }
                setPendingTransfers(await response.json());
            } catch (err) {
                setError(err.message || "Failed to load book requests.");
                setPendingTransfers([]);
            } finally {
                setLoadingTransfers(false);
            }
        };

        const fetchSourceStock = async (libraryId) => {
            if (!libraryId) {
                setSourceStock([]);
                return;
            }

            setLoadingSourceStock(true);
            try {
                const response = await fetch(
                    `${API}/api/stock/${encodeURIComponent(libraryId)}`,
                    { headers: { Authorization: `Bearer ${token}` } }
                );
                if (!response.ok) {
                    throw new Error(`Failed to load source library stock: ${response.status}`);
                }
                setSourceStock(await response.json());
            } catch (err) {
                setError(err.message || "Failed to load source library stock.");
                setSourceStock([]);
            } finally {
                setLoadingSourceStock(false);
            }
        };

        useEffect(() => {
            fetchLibraries();
            fetchBooks();
        }, []);

        useEffect(() => {
            if (selectedLibraryId) {
                fetchStock(selectedLibraryId);
                fetchPendingTransfers(selectedLibraryId);
            }
        }, [selectedLibraryId]);

        /*
         * Map book ID -> catalog book.
         */
        const bookMap = useMemo(() => {
            const map = new Map();

            books.forEach((book) => {
                const id = getBookId(book);

                if (id) {
                    map.set(String(id), book);
                }
            });

            return map;
        }, [books]);

        /*
         * Add the catalog book information to every stock row.
         */
        const stockRows = useMemo(() => {
            return stock.map((stockItem) => {
                const bookId = getStockBookId(stockItem);
                const book = bookMap.get(String(bookId));

                return {
                    ...stockItem,
                    book,
                    bookId,
                };
            });
        }, [stock, bookMap]);

        /*
         * IDs of books that are already stocked in the selected library.
         */
        const stockedBookIds = useMemo(() => {
            return new Set(
                stock.map((stockItem) => String(getStockBookId(stockItem)))
            );
        }, [stock]);

        /*
         * Books available to add to this library.
         */
        const filteredBooks = useMemo(() => {
            const search = bookSearch.trim().toLowerCase();

            return books
                .filter((book) => {
                    const id = getBookId(book);

                    if (stockedBookIds.has(String(id))) {
                        return false;
                    }

                    if (!search) {
                        return true;
                    }

                    const title = book.title?.toLowerCase() ?? "";
                    const author = book.author?.toLowerCase() ?? "";
                    const isbn = book.isbn?.toLowerCase() ?? "";

                    return (
                        title.includes(search) ||
                        author.includes(search) ||
                        isbn.includes(search)
                    );
                })
                .slice(0, 20);
        }, [books, bookSearch, stockedBookIds]);

        const filteredMembers = useMemo(() => {
            const search = memberSearch.trim().toLowerCase();
            if (!search) return members;

            return members.filter((member) =>
                member.membershipNumber?.toLowerCase().includes(search)
            );
        }, [members, memberSearch]);

        const openStockModal = (type, stockItem) => {
            setModal({
                type,
                stock: stockItem,
            });

            setQuantity(1);
            setError("");
        };

        const openNewStockModal = () => {
            setModal({
                type: "new-stock",
            });

            setBookSearch("");
            setSelectedBook(null);
            setQuantity(1);
            setError("");
        };

        const openLoanModal = async (stockItem) => {
            setModal({ type: "loan", stock: stockItem });
            setMemberSearch("");
            setSelectedMemberId("");
            setError("");
            setLoadingMembers(true);

            try {
                const response = await fetch(`${API}/api/members/all`, {
                    headers: { Authorization: `Bearer ${token}` },
                });
                if (!response.ok) {
                    throw new Error(`Failed to load members: ${response.status}`);
                }
                setMembers(await response.json());
            } catch (err) {
                setError(err.message || "Failed to load members.");
                setMembers([]);
            } finally {
                setLoadingMembers(false);
            }
        };

        const openTransferModal = () => {
            setModal({ type: "transfer" });
            setSourceLibraryId("");
            setSourceStock([]);
            setRequestedBookId("");
            setQuantity(1);
            setError("");
        };

        const closeModal = () => {
            if (saving) return;

            setModal(null);
            setQuantity(1);
            setBookSearch("");
            setSelectedBook(null);
            setMemberSearch("");
            setSelectedMemberId("");
            setSourceLibraryId("");
            setSourceStock([]);
            setRequestedBookId("");
        };

        const currentUser = () =>
            keycloak.tokenParsed?.preferred_username ??
            keycloak.tokenParsed?.email ??
            "library-staff";

        const handleCreateTransfer = async () => {
            if (!selectedLibraryId || !sourceLibraryId || !requestedBookId) {
                setError("Please select a supplying library and book.");
                return;
            }

            const amount = Number(quantity);
            const sourceBook = sourceStock.find(
                (item) => String(getStockBookId(item)) === String(requestedBookId)
            );
            if (!Number.isInteger(amount) || amount < 1 || amount > (sourceBook?.availableQuantity ?? 0)) {
                setError(`Quantity must be between 1 and ${sourceBook?.availableQuantity ?? 0}.`);
                return;
            }

            setSaving(true);
            setError("");
            try {
                const response = await fetch(`${API}/api/transfers/request`, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${token}`,
                    },
                    body: JSON.stringify({
                        sourceLibraryId,
                        destinationLibraryId: selectedLibraryId,
                        bookId: requestedBookId,
                        quantity: amount,
                        requestedBy: currentUser(),
                        requestedAt: new Date().toISOString(),
                    }),
                });
                if (!response.ok) {
                    const message = await response.text();
                    throw new Error(message || "Failed to request books.");
                }

                setModal(null);
                setSourceLibraryId("");
                setSourceStock([]);
                setRequestedBookId("");
            } catch (err) {
                setError(err.message || "Failed to request books.");
            } finally {
                setSaving(false);
            }
        };

        const handleReviewTransfer = async (transfer, accepted) => {
            const availableBefore = stock.find(
                (item) => String(getStockBookId(item)) === String(transfer.titleId)
            )?.availableQuantity ?? 0;

            setSaving(true);
            setError("");

            try {
                const reviewResponse = await fetch(
                    `${API}/api/transfers/${accepted ? "accept" : "reject"}`,
                    {
                        method: "POST",
                        headers: {
                            "Content-Type": "application/json",
                            Authorization: `Bearer ${token}`,
                        },
                        body: JSON.stringify({
                            id: transfer.id,
                            reviewedBy: currentUser(),
                            reviewedAt: new Date().toISOString(),
                        }),
                    }
                );
                if (!reviewResponse.ok) {
                    const message = await reviewResponse.text();
                    throw new Error(message || `Failed to ${accepted ? "accept" : "reject"} transfer.`);
                }

                if (accepted) {
                    const expectedAvailable = Math.max(
                        0,
                        availableBefore - transfer.quantity
                    );
                    for (let attempt = 0; attempt < 10; attempt += 1) {
                        await new Promise((resolve) => setTimeout(resolve, 300));
                        const updatedStock = await fetchStock(selectedLibraryId, false);
                        const updatedAvailable = updatedStock?.find(
                            (item) => String(getStockBookId(item)) === String(transfer.titleId)
                        )?.availableQuantity ?? 0;

                        if (updatedAvailable <= expectedAvailable) {
                            break;
                        }
                    }
                } else {
                    await fetchStock(selectedLibraryId);
                }

                await fetchPendingTransfers(selectedLibraryId);
            } catch (err) {
                setError(err.message || "Failed to review transfer.");
            } finally {
                setSaving(false);
            }
        };

        const handleCreateLoan = async () => {
            if (!modal?.stock || !selectedLibraryId || !selectedMemberId) {
                setError("Please select a member.");
                return;
            }

            const bookId = getStockBookId(modal.stock);
            if (!bookId) {
                setError("The selected book does not have a valid ID.");
                return;
            }

            setSaving(true);
            setError("");
            try {
                const response = await fetch(`${API}/api/loans/create`, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${token}`,
                    },
                    body: JSON.stringify({
                        memberId: selectedMemberId,
                        libraryId: selectedLibraryId,
                        bookId,
                        borrowedAt: new Date().toISOString(),
                        idempotencyKey: crypto.randomUUID(),
                    }),
                });
                if (!response.ok) {
                    const message = await response.text();
                    throw new Error(message || "Failed to create loan.");
                }

                const createdLoan = await response.json();
                const loanId = createdLoan.id;

                if (!loanId) {
                    throw new Error("Loan was accepted but no loan ID was returned.");
                }

                let loanReady = false;
                for (let attempt = 0; attempt < 10; attempt += 1) {
                    await new Promise((resolve) => setTimeout(resolve, 300));

                    const loanResponse = await fetch(`${API}/api/loans/${loanId}`, {
                        headers: { Authorization: `Bearer ${token}` },
                    });

                    if (loanResponse.ok) {
                        loanReady = true;
                        break;
                    }
                }

                if (!loanReady) {
                    throw new Error("Loan is still being processed. Please refresh in a moment.");
                }

                await fetchStock(selectedLibraryId);
                setModal(null);
                setMemberSearch("");
                setSelectedMemberId("");
            } catch (err) {
                setError(err.message || "Failed to create loan.");
            } finally {
                setSaving(false);
            }
        };

        const handleAddNewStock = async () => {
            if (!selectedLibraryId || !selectedBook) {
                setError("Please select a book.");
                return;
            }

            const amount = Number(quantity);

            if (!Number.isInteger(amount) || amount < 1 || amount > 1_000_000) {
                setError("Quantity must be between 1 and 1,000,000.");
                return;
            }

            const bookId = getBookId(selectedBook);

            if (!bookId) {
                setError("The selected book does not have a valid ID.");
                return;
            }

            const body = {
                libraryId: selectedLibraryId,
                bookId: bookId,
                quantity: amount,
            };

            setSaving(true);
            setError("");

            try {
                const response = await fetch(`${API}/api/stock`, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${token}`,
                    },
                    body: JSON.stringify(body),
                });

                if (!response.ok) {
                    let message = `Request failed: ${response.status}`;

                    try {
                        const data = await response.json();

                        if (data.detail) {
                            message = data.detail;
                        } else if (data.message) {
                            message = data.message;
                        }
                    } catch {
                        // Keep default message.
                    }

                    throw new Error(message);
                }

                await fetchStock(selectedLibraryId);

                closeModal();
            } catch (err) {
                setError(err.message);
            } finally {
                setSaving(false);
            }
        };

        const handleStockMutation = async () => {
            if (!modal || !selectedLibraryId || !modal.stock) {
                return;
            }

            const amount = Number(quantity);

            if (!Number.isInteger(amount) || amount < 1) {
                setError("Quantity must be at least 1.");
                return;
            }

            if (
                modal.type === "remove" &&
                amount > modal.stock.availableQuantity
            ) {
                setError(
                    `You can only remove up to ${modal.stock.availableQuantity} available copies.`
                );
                return;
            }

            const body = {
                libraryId: selectedLibraryId,
                bookId: getStockBookId(modal.stock),
                quantity: amount,
            };

            setSaving(true);
            setError("");

            try {
                const response = await fetch(`${API}/api/stock`, {
                    method: modal.type === "add" ? "POST" : "DELETE",
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${token}`,
                    },
                    body: JSON.stringify(body),
                });

                if (!response.ok) {
                    let message = `Request failed: ${response.status}`;

                    try {
                        const data = await response.json();

                        if (data.detail) {
                            message = data.detail;
                        } else if (data.message) {
                            message = data.message;
                        }
                    } catch {
                        // Keep default message.
                    }

                    throw new Error(message);
                }

                await fetchStock(selectedLibraryId);

                closeModal();
            } catch (err) {
                setError(err.message);
            } finally {
                setSaving(false);
            }
        };

        if (loadingLibraries || loadingBooks) {
            return (
                <div className={styles.page}>
                    <p>Loading inventory...</p>
                </div>
            );
        }

        return (
            <div className={styles.page}>
                {/* HEADER */}

                <div className={styles.header}>
                    <div>
                        <h1 className={styles.title}>Library Stocks</h1>

                        <p className={styles.subtitle}>
                            Manage the stock of books in each library.
                        </p>
                    </div>

                    <div className={styles.headerActions}>
                        <button
                            type="button"
                            className={styles.secondaryButton}
                            onClick={() => navigate("/libraries")}
                        >
                            Manage Libraries
                        </button>

                        <button
                            className={styles.secondaryButton}
                            onClick={openTransferModal}
                        >
                            Request Copies
                        </button>

                        <button
                            className={styles.primaryButton}
                            onClick={openNewStockModal}
                        >
                            + Add Book
                        </button>
                    </div>
                </div>
                {error && <div className={styles.error}>{error}</div>}

                {/* LIBRARY SELECTOR */}

                <div className={styles.librarySelector}>
                    <label htmlFor="library-select">Library:</label>

                    <select
                        id="library-select"
                        className={styles.librarySelect}
                        value={selectedLibraryId}
                        onChange={(event) => {
                            setSelectedLibraryId(event.target.value);
                        }}
                    >
                        {libraries.map((library) => {
                            const id = getLibraryId(library);

                            return (
                                <option key={id} value={id}>
                                    {library.name}
                                </option>
                            );
                        })}
                    </select>
                </div>

                {/* STOCK TABLE */}

                {loadingStock ? (
                    <p>Loading stock...</p>
                ) : stockRows.length === 0 ? (
                    <div className={styles.emptyState}>
                        <h2>No stock yet</h2>
                        <p>
                            This library currently has no stock. Click{" "}
                            <strong>+ Add Book</strong> to add one.
                        </p>
                    </div>
                ) : (
                    <div className={styles.tableWrap}>
                        <table className={styles.table}>
                            <thead>
                            <tr>
                                <th>Book</th>
                                <th>Author</th>
                                <th>ISBN</th>
                                <th className={styles.center}>Total</th>
                                <th className={styles.center}>Available</th>
                                <th className={styles.center}>Borrowed</th>
                                <th className={styles.center}>Actions</th>
                            </tr>
                            </thead>

                            <tbody>
                            {stockRows.map((item) => (
                                <tr key={item.bookId}>
                                    <td>
                                        {item.book ? (
                                            <a
                                                className={styles.bookLink}
                                                href={`/books/${item.bookId}`}
                                            >
                                                {item.book.title}
                                            </a>
                                        ) : (
                                            <span className={styles.muted}>Unknown book</span>
                                        )}
                                    </td>

                                    <td>
                                        {item.book?.author ?? (
                                            <span className={styles.muted}>—</span>
                                        )}
                                    </td>

                                    <td>
                                        {item.book?.isbn ?? (
                                            <span className={styles.muted}>—</span>
                                        )}
                                    </td>

                                    <td className={styles.center}>
                                        {item.totalQuantity}
                                    </td>

                                    <td className={styles.center}>
                                        {item.availableQuantity}
                                    </td>

                                    <td className={styles.center}>
                                        {item.borrowedQuantity}
                                    </td>

                                    <td className={styles.center}>
                                        <div className={styles.rowActions}>
                                            <button
                                                className={styles.rowButton}
                                                onClick={() => openStockModal("add", item)}
                                            >
                                                Add
                                            </button>

                                            <button
                                                className={styles.rowButtonDanger}
                                                onClick={() => openStockModal("remove", item)}
                                                disabled={item.availableQuantity === 0}
                                            >
                                                Remove
                                            </button>

                                            <button
                                                className={styles.rowButtonDark}
                                                onClick={() => openLoanModal(item)}
                                                disabled={item.availableQuantity === 0}
                                            >
                                                Loan
                                            </button>

                                        </div>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}

                {/* =========================
              MODAL: NEW STOCK
              ========================= */}

                {modal && modal.type === "new-stock" && (
                    <div className={styles.modalOverlay}>
                        <div className={styles.modal}>
                            <h2 className={styles.modalTitle}>
                                Add Book to Stock
                            </h2>

                            <div className={styles.field}>
                                <label>Search books</label>

                                <input
                                    className={styles.input}
                                    type="text"
                                    value={bookSearch}
                                    onChange={(event) => {
                                        setBookSearch(event.target.value);
                                        setSelectedBook(null);
                                    }}
                                    placeholder="Search by title, author or ISBN..."
                                />
                            </div>

                            <div className={styles.bookList}>
                                {filteredBooks.length === 0 ? (
                                    <p className={styles.bookListEmpty}>
                                        No books found.
                                    </p>
                                ) : (
                                    filteredBooks.map((book) => {
                                        const id = getBookId(book);
                                        const isSelected =
                                            selectedBook &&
                                            String(getBookId(selectedBook)) === String(id);

                                        return (
                                            <button
                                                key={id}
                                                type="button"
                                                onClick={() => setSelectedBook(book)}
                                                className={`${styles.bookItem} ${
                                                    isSelected ? styles.bookItemSelected : ""
                                                }`}
                                            >
                                                <strong>{book.title}</strong>

                                                <br />

                                                <span className={styles.bookItemAuthor}>
                            {book.author ?? "Unknown author"}
                          </span>

                                                {book.isbn && (
                                                    <>
                                                        <br />
                                                        <span className={styles.bookItemIsbn}>
                                ISBN: {book.isbn}
                              </span>
                                                    </>
                                                )}
                                            </button>
                                        );
                                    })
                                )}
                            </div>

                            {selectedBook && (
                                <div className={styles.selectedBook}>
                                    <strong>Selected: {selectedBook.title}</strong>
                                    <br />
                                    {selectedBook.author}
                                    {selectedBook.isbn && (
                                        <>
                                            <br />
                                            ISBN: {selectedBook.isbn}
                                        </>
                                    )}
                                </div>
                            )}

                            <div className={styles.field}>
                                <label>Quantity</label>

                                <input
                                    className={styles.input}
                                    type="number"
                                    min="1"
                                    max="1000000"
                                    value={quantity}
                                    onChange={(event) =>
                                        setQuantity(event.target.value)
                                    }
                                />
                            </div>

                            <div className={styles.modalActions}>
                                <button
                                    className={styles.secondaryButton}
                                    onClick={closeModal}
                                    disabled={saving}
                                >
                                    Cancel
                                </button>

                                <button
                                    className={styles.primaryButton}
                                    onClick={handleAddNewStock}
                                    disabled={saving || !selectedBook}
                                >
                                    {saving ? "Adding..." : "Add Book"}
                                </button>
                            </div>
                        </div>
                    </div>
                )}

                <section className={styles.pendingTransfers}>
                    <h2>Book Requests From Other Libraries</h2>
                    {loadingTransfers ? (
                        <p>Loading book requests...</p>
                    ) : pendingTransfers.length === 0 ? (
                        <p>No book requests for this library.</p>
                    ) : (
                        pendingTransfers.map((transfer) => (
                            <div key={transfer.id} className={styles.transferRequest}>
                                <p>
                                    <strong>{transfer.quantity}</strong> copy/copies of {bookMap.get(
                                        String(transfer.titleId)
                                    )?.title ?? `book ${transfer.titleId}`}
                                </p>
                                <p>
                                    Requested by library:{" "}
                                    {libraries.find(
                                        (library) =>
                                            String(getLibraryId(library)) ===
                                            String(transfer.destinationLibraryId)
                                    )?.name ?? "Unknown library"}
                                </p>
                                <div className={styles.rowActions}>
                                    <button
                                        className={styles.primaryButton}
                                        onClick={() => handleReviewTransfer(transfer, true)}
                                        disabled={saving}
                                    >
                                        Accept Request
                                    </button>
                                    <button
                                        className={styles.rowButtonDanger}
                                        onClick={() => handleReviewTransfer(transfer, false)}
                                        disabled={saving}
                                    >
                                        Decline
                                    </button>
                                </div>
                            </div>
                        ))
                    )}
                </section>

                {modal && modal.type === "loan" && (
                    <div className={styles.modalOverlay}>
                        <div className={`${styles.modal} ${styles.modalSmall}`}>
                            <h2 className={styles.modalTitle}>Create Loan</h2>

                            <div className={styles.selectedBook}>
                                <strong>{modal.stock.book?.title ?? "Selected book"}</strong>
                                <br />
                                Library: {libraries.find((library) =>
                                    String(getLibraryId(library)) === String(selectedLibraryId)
                                )?.name ?? selectedLibraryId}
                            </div>

                            <div className={styles.field}>
                                <label htmlFor="member-membership-search">
                                    Search by membership ID
                                </label>
                                <input
                                    id="member-membership-search"
                                    className={styles.input}
                                    type="search"
                                    value={memberSearch}
                                    onChange={(event) => {
                                        setMemberSearch(event.target.value);
                                        setSelectedMemberId("");
                                    }}
                                    placeholder="Enter membership ID..."
                                    disabled={loadingMembers || saving}
                                />
                            </div>

                            <div className={styles.field}>
                                <label htmlFor="loan-member">Member</label>
                                <select
                                    id="loan-member"
                                    className={styles.librarySelect}
                                    value={selectedMemberId}
                                    onChange={(event) => setSelectedMemberId(event.target.value)}
                                    disabled={loadingMembers || saving}
                                >
                                    <option value="">
                                        {loadingMembers ? "Loading members..." : "Select a member"}
                                    </option>
                                    {filteredMembers.map((member) => (
                                        <option key={member.memberId} value={member.memberId}>
                                            {member.membershipNumber} — {member.firstName} {member.lastName}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            {!loadingMembers && memberSearch && filteredMembers.length === 0 && (
                                <p className={styles.bookListEmpty}>No members found.</p>
                            )}

                            <div className={styles.modalActions}>
                                <button
                                    className={styles.secondaryButton}
                                    onClick={closeModal}
                                    disabled={saving}
                                >
                                    Cancel
                                </button>
                                <button
                                    className={styles.primaryButton}
                                    onClick={handleCreateLoan}
                                    disabled={loadingMembers || saving || !selectedMemberId}
                                >
                                    {saving ? "Creating..." : "Create Loan"}
                                </button>
                            </div>
                        </div>
                    </div>
                )}

                {modal && modal.type === "transfer" && (
                    <div className={styles.modalOverlay}>
                        <div className={`${styles.modal} ${styles.modalSmall}`}>
                            <h2 className={styles.modalTitle}>Request Copies</h2>

                            <div className={styles.selectedBook}>
                                Request copies for: {libraries.find((library) =>
                                    String(getLibraryId(library)) === String(selectedLibraryId)
                                )?.name ?? selectedLibraryId}
                            </div>

                            <div className={styles.field}>
                                <label htmlFor="transfer-source">Library to supply the books</label>
                                <select
                                    id="transfer-source"
                                    className={styles.librarySelect}
                                    value={sourceLibraryId}
                                    onChange={(event) => {
                                        const libraryId = event.target.value;
                                        setSourceLibraryId(libraryId);
                                        setRequestedBookId("");
                                        fetchSourceStock(libraryId);
                                    }}
                                    disabled={saving}
                                >
                                    <option value="">Select a supplying library</option>
                                    {libraries
                                        .filter((library) => String(getLibraryId(library)) !== String(selectedLibraryId))
                                        .map((library) => (
                                            <option key={getLibraryId(library)} value={getLibraryId(library)}>
                                                {library.name}
                                            </option>
                                        ))}
                                </select>
                            </div>

                            <div className={styles.field}>
                                <label htmlFor="transfer-book">Book</label>
                                <select
                                    id="transfer-book"
                                    className={styles.librarySelect}
                                    value={requestedBookId}
                                    onChange={(event) => setRequestedBookId(event.target.value)}
                                    disabled={!sourceLibraryId || loadingSourceStock || saving}
                                >
                                    <option value="">
                                        {loadingSourceStock ? "Loading available books..." : "Select a book"}
                                    </option>
                                    {sourceStock
                                        .filter((item) => item.availableQuantity > 0)
                                        .map((item) => {
                                            const bookId = getStockBookId(item);
                                            const book = bookMap.get(String(bookId));
                                            return (
                                                <option key={bookId} value={bookId}>
                                                    {book?.title ?? `Book ${bookId}`} ({item.availableQuantity} available)
                                                </option>
                                            );
                                        })}
                                </select>
                            </div>

                            <div className={styles.field}>
                                <label htmlFor="transfer-quantity">Quantity</label>
                                <input
                                    id="transfer-quantity"
                                    className={styles.input}
                                    type="number"
                                    min="1"
                                    max={sourceStock.find(
                                        (item) => String(getStockBookId(item)) === String(requestedBookId)
                                    )?.availableQuantity ?? 1}
                                    value={quantity}
                                    onChange={(event) => setQuantity(event.target.value)}
                                    disabled={!requestedBookId || saving}
                                />
                            </div>

                            <div className={styles.modalActions}>
                                <button
                                    className={styles.secondaryButton}
                                    onClick={closeModal}
                                    disabled={saving}
                                >
                                    Cancel
                                </button>
                                <button
                                    className={styles.primaryButton}
                                    onClick={handleCreateTransfer}
                                    disabled={saving || !sourceLibraryId || !requestedBookId || loadingSourceStock}
                                >
                                    {saving ? "Requesting..." : "Send Request"}
                                </button>
                            </div>
                        </div>
                    </div>
                )}

                {/* =========================
              MODAL: ADD / REMOVE COPIES
              ========================= */}

                {modal &&
                    (modal.type === "add" || modal.type === "remove") && (
                        <div className={styles.modalOverlay}>
                            <div className={`${styles.modal} ${styles.modalSmall}`}>
                                <h2 className={styles.modalTitle}>
                                    {modal.type === "add" ? "Add Copies" : "Remove Copies"}
                                </h2>

                                <p>
                                    <strong>{modal.stock.book?.title ?? "Book"}</strong>
                                </p>

                                {modal.type === "remove" && (
                                    <p>
                                        Available copies: {modal.stock.availableQuantity}
                                    </p>
                                )}

                                <div className={styles.field}>
                                    <label>Quantity</label>

                                    <input
                                        className={styles.input}
                                        type="number"
                                        min="1"
                                        max={
                                            modal.type === "remove"
                                                ? modal.stock.availableQuantity
                                                : 1000000
                                        }
                                        value={quantity}
                                        onChange={(event) =>
                                            setQuantity(event.target.value)
                                        }
                                    />
                                </div>

                                <div className={styles.modalActions}>
                                    <button
                                        className={styles.secondaryButton}
                                        onClick={closeModal}
                                        disabled={saving}
                                    >
                                        Cancel
                                    </button>

                                    <button
                                        className={styles.primaryButton}
                                        onClick={handleStockMutation}
                                        disabled={saving}
                                    >
                                        {saving
                                            ? "Saving..."
                                            : modal.type === "add"
                                                ? "Add Copies"
                                                : "Remove Copies"}
                                    </button>
                                </div>
                            </div>
                        </div>
                    )}
            </div>
        );
    }

    export default StockPage;
