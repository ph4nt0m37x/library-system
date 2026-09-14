import { useEffect, useMemo, useState } from "react";
import styles from "../styles/StockPage.module.css";

const API = "http://localhost:8000";

function StockPage() {
    const [libraries, setLibraries] = useState([]);
    const [books, setBooks] = useState([]);
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
                setSelectedLibraryId(getLibraryId(data[0]));
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

    const fetchStock = async (libraryId) => {
        if (!libraryId) {
            setStock([]);
            return;
        }

        setLoadingStock(true);
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
        } catch (err) {
            setError(err.message);
            setStock([]);
        } finally {
            setLoadingStock(false);
        }
    };

    useEffect(() => {
        fetchLibraries();
        fetchBooks();
    }, []);

    useEffect(() => {
        if (selectedLibraryId) {
            fetchStock(selectedLibraryId);
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

    const closeModal = () => {
        if (saving) return;

        setModal(null);
        setQuantity(1);
        setBookSearch("");
        setSelectedBook(null);
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
                    <h1 className={styles.title}>Book Stock</h1>

                    <p className={styles.subtitle}>
                        Manage the stock of books in each library.
                    </p>
                </div>

                <button
                    className={styles.primaryButton}
                    onClick={openNewStockModal}
                >
                    + Add Book
                </button>
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

                                    <button className={styles.rowButton} disabled>
                                        Loan
                                    </button>
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