import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import keycloak from "../keycloak";
import styles from "../styles/BooksPage.module.css";

function BooksPage() {
  const navigate = useNavigate();
  const [books, setBooks] = useState([]);
  const [categories, setCategories] = useState([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [searchTerm, setSearchTerm] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("");

  const [showForm, setShowForm] = useState(false);
  const [editingBook, setEditingBook] = useState(null);

  const [form, setForm] = useState({
    isbn: "",
    title: "",
    author: "",
    description: "",
    publicationYear: "",
    price: "",
    categoryId: "",
  });

  // =========================
  // LOAD BOOKS
  // =========================

  const loadBooks = async () => {
    try {
      const response = await fetch(
          "http://localhost:8000/api/books/available",
          {
            headers: {
              Authorization: `Bearer ${keycloak.token}`,
            },
          }
      );

      if (!response.ok) {
        throw new Error(`Failed to fetch books: ${response.status}`);
      }

      const data = await response.json();
      setBooks(data);
    } catch (error) {
      console.error(error);
      setError("Could not load books.");
    } finally {
      setLoading(false);
    }
  };

  // =========================
  // LOAD CATEGORIES
  // =========================

  const loadCategories = async () => {
    try {
      const response = await fetch(
          "http://localhost:8000/api/categories/all",
          {
            headers: {
              Authorization: `Bearer ${keycloak.token}`,
            },
          }
      );

      if (!response.ok) {
        throw new Error(
            `Failed to fetch categories: ${response.status}`
        );
      }

      const data = await response.json();
      setCategories(data);
    } catch (error) {
      console.error(error);
    }
  };

  useEffect(() => {
    loadBooks();
    loadCategories();
  }, []);

  // =========================
  // FORM CHANGE
  // =========================

  const handleChange = (event) => {
    setForm({
      ...form,
      [event.target.name]: event.target.value,
    });
  };

  // =========================
  // CREATE
  // =========================

  const handleCreate = async (event) => {
    event.preventDefault();

    try {
      const response = await fetch(
          "http://localhost:8000/api/books/create",
          {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${keycloak.token}`,
            },
            body: JSON.stringify({
              isbn: form.isbn,
              title: form.title,
              author: form.author,
              description: form.description || null,
              publicationYear: form.publicationYear
                  ? Number(form.publicationYear)
                  : null,
              price: Number(form.price),
              categoryId: form.categoryId
                  ? Number(form.categoryId)
                  : null,
            }),
          }
      );

      if (!response.ok) {
        const errorText = await response.text();

        console.error(
            "Create book error:",
            response.status,
            errorText
        );

        throw new Error(
            `Failed to create book: ${response.status}`
        );
      }

      resetForm();
      await loadBooks();
    } catch (error) {
      console.error(error);
      setError("Could not create book.");
    }
  };

  // =========================
  // START UPDATE
  // =========================

  const startEditing = (book) => {
    setEditingBook(book);

    const bookCategoryId =
        book.category?.id ??
        book.categoryId ??
        "";

    setForm({
      isbn: book.isbn ?? "",
      title: book.title ?? "",
      author: book.author ?? "",
      description: book.description ?? "",
      publicationYear: book.publicationYear ?? "",
      price: book.price?.amount ?? book.price ?? "",
      categoryId: bookCategoryId,
    });

    setShowForm(false);
  };

  // =========================
  // UPDATE
  // =========================

  const handleUpdate = async (event) => {
    event.preventDefault();

    const bookId =
        editingBook.id?.value ??
        editingBook.id?.id ??
        editingBook.id;

    try {
      const response = await fetch(
          "http://localhost:8000/api/books/update",
          {
            method: "PUT",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${keycloak.token}`,
            },
            body: JSON.stringify({
              id: bookId,
              isbn: form.isbn,
              title: form.title,
              author: form.author,
              description: form.description || null,
              publicationYear: form.publicationYear
                  ? Number(form.publicationYear)
                  : null,
              price: Number(form.price),
              categoryId: form.categoryId
                  ? Number(form.categoryId)
                  : null,
            }),
          }
      );

      if (!response.ok) {
        const errorText = await response.text();

        console.error(
            "Update book error:",
            response.status,
            errorText
        );

        throw new Error(
            `Failed to update book: ${response.status}`
        );
      }

      resetForm();
      await loadBooks();
    } catch (error) {
      console.error(error);
      setError("Could not update book.");
    }
  };

  // =========================
  // DELETE
  // =========================

  const handleDelete = async (book) => {
    const bookId =
        book.id?.value ??
        book.id?.id ??
        book.id;

    const confirmed = window.confirm(
        `Are you sure you want to delete "${book.title}"?`
    );

    if (!confirmed) {
      return;
    }

    try {
      const response = await fetch(
          "http://localhost:8000/api/books/delete",
          {
            method: "DELETE",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${keycloak.token}`,
            },
            body: JSON.stringify({
              id: bookId,
            }),
          }
      );

      if (!response.ok) {
        const errorText = await response.text();

        console.error(
            "Delete book error:",
            response.status,
            errorText
        );

        throw new Error(
            `Failed to delete book: ${response.status}`
        );
      }

      await loadBooks();
    } catch (error) {
      console.error(error);
      setError("Could not delete book.");
    }
  };

  // =========================
  // RESET FORM
  // =========================

  const resetForm = () => {
    setForm({
      isbn: "",
      title: "",
      author: "",
      description: "",
      publicationYear: "",
      price: "",
      categoryId: "",
    });

    setShowForm(false);
    setEditingBook(null);
    setError("");
  };

  // =========================
  // HELPERS
  // =========================

  const getBookId = (book) =>
      book.id?.value ??
      book.id?.id ??
      book.id;

  const getPrice = (book) =>
      book.price?.amount ??
      book.price ??
      "N/A";

  const getCategoryName = (book) => {
    if (book.category?.name) {
      return book.category.name;
    }

    if (book.categoryId) {
      const category = categories.find(
          (category) =>
              Number(category.id) === Number(book.categoryId)
      );

      return (
          category?.name ??
          `Category ${book.categoryId}`
      );
    }

    return "No category";
  };

  // =========================
  // FILTER BOOKS
  // =========================

  const filteredBooks = books.filter((book) => {
    const search = searchTerm
        .toLowerCase()
        .trim();

    const matchesSearch =
        !search ||
        book.title?.toLowerCase().includes(search) ||
        book.author?.toLowerCase().includes(search) ||
        book.isbn?.toLowerCase().includes(search);

    const categoryId =
        book.category?.id ??
        book.categoryId;

    const matchesCategory =
        !selectedCategory ||
        Number(categoryId) ===
        Number(selectedCategory);

    return matchesSearch && matchesCategory;
  });

  // =========================
  // LOADING / ERROR
  // =========================

  if (loading) {
    return (
        <div className={styles.page}>
          <p>Loading books...</p>
        </div>
    );
  }

  if (error) {
    return (
        <div className={styles.page}>
          <p className={styles.error}>{error}</p>
        </div>
    );
  }

  // =========================
  // PAGE
  // =========================

  return (
      <div className={styles.page}>
        <div className={styles.header}>
          <div>
            <h1 className={styles.title}>Books</h1>

            <p className={styles.subtitle}>
              Browse and manage the library catalogue.
            </p>
          </div>

          {!editingBook && (
              <button
                  className={styles.primaryButton}
                  onClick={() =>
                      setShowForm(!showForm)
                  }
              >
                {showForm
                    ? "Cancel"
                    : "+ Add Book"}
              </button>
          )}
        </div>

        {/* =========================
          SEARCH & FILTERS
          ========================= */}

        <div className={styles.searchSection}>
          <div className={styles.searchRow}>
            <input
                type="text"
                value={searchTerm}
                onChange={(event) =>
                    setSearchTerm(event.target.value)
                }
                placeholder="Search by title, author, or ISBN..."
                className={styles.searchInput}
            />

            <select
                value={selectedCategory}
                onChange={(event) =>
                    setSelectedCategory(
                        event.target.value
                    )
                }
                className={styles.categoryFilter}
            >
              <option value="">
                All categories
              </option>

              {categories.map((category) => (
                  <option
                      key={category.id}
                      value={category.id}
                  >
                    {category.name}
                  </option>
              ))}
            </select>
          </div>

          {(searchTerm || selectedCategory) && (
              <p className={styles.resultText}>
                Showing {filteredBooks.length}{" "}
                {filteredBooks.length === 1
                    ? "book"
                    : "books"}
              </p>
          )}
        </div>

        {/* =========================
          CREATE FORM
          ========================= */}

        {showForm && !editingBook && (
            <form
                onSubmit={handleCreate}
                className={styles.formCard}
            >
              <h2 className={styles.formTitle}>
                Add New Book
              </h2>

              <div className={styles.formGrid}>
                <div className={styles.field}>
                  <label>ISBN</label>

                  <input
                      name="isbn"
                      value={form.isbn}
                      onChange={handleChange}
                      placeholder="ex. 9781234567890"
                      required
                  />
                </div>

                <div className={styles.field}>
                  <label>Title</label>

                  <input
                      name="title"
                      value={form.title}
                      onChange={handleChange}
                      placeholder="Book title"
                      required
                  />
                </div>

                <div className={styles.field}>
                  <label>Author</label>

                  <input
                      name="author"
                      value={form.author}
                      onChange={handleChange}
                      placeholder="Author name"
                      required
                  />
                </div>

                <div className={styles.field}>
                  <label>Category</label>

                  <select
                      name="categoryId"
                      value={form.categoryId}
                      onChange={handleChange}
                  >
                    <option value="">
                      Select a category
                    </option>

                    {categories.map((category) => (
                        <option
                            key={category.id}
                            value={category.id}
                        >
                          {category.name}
                        </option>
                    ))}
                  </select>
                </div>

                <div className={styles.field}>
                  <label>Publication Year</label>

                  <input
                      type="number"
                      name="publicationYear"
                      value={form.publicationYear}
                      onChange={handleChange}
                      placeholder="ex. 2026"
                  />
                </div>

                <div className={styles.field}>
                  <label>Price</label>

                  <input
                      type="number"
                      step="0.01"
                      name="price"
                      value={form.price}
                      onChange={handleChange}
                      placeholder="ex. 1099"
                      required
                  />
                </div>

                <div className={styles.fieldFull}>
                  <label>Description</label>

                  <textarea
                      name="description"
                      value={form.description}
                      onChange={handleChange}
                      placeholder="Short description of the book..."
                      rows="4"
                  />
                </div>
              </div>

              <div className={styles.formActions}>
                <button
                    type="submit"
                    className={styles.primaryButton}
                >
                  Create Book
                </button>

                <button
                    type="button"
                    onClick={resetForm}
                    className={styles.secondaryButton}
                >
                  Cancel
                </button>
              </div>
            </form>
        )}

        {/* =========================
          UPDATE FORM
          ========================= */}

        {editingBook && (
            <form
                onSubmit={handleUpdate}
                className={styles.formCard}
            >
              <div className={styles.editHeader}>
                <div>
                  <h2 className={styles.formTitle}>
                    Update Book
                  </h2>

                </div>
              </div>

              <div className={styles.formGrid}>
                <div className={styles.field}>
                  <label>ISBN</label>

                  <input
                      name="isbn"
                      value={form.isbn}
                      onChange={handleChange}
                      required
                  />
                </div>

                <div className={styles.field}>
                  <label>Title</label>

                  <input
                      name="title"
                      value={form.title}
                      onChange={handleChange}
                      required
                  />
                </div>

                <div className={styles.field}>
                  <label>Author</label>

                  <input
                      name="author"
                      value={form.author}
                      onChange={handleChange}
                      required
                  />
                </div>

                <div className={styles.field}>
                  <label>Category</label>

                  <select
                      name="categoryId"
                      value={form.categoryId}
                      onChange={handleChange}
                  >
                    <option value="">
                      No category
                    </option>

                    {categories.map((category) => (
                        <option
                            key={category.id}
                            value={category.id}
                        >
                          {category.name}
                        </option>
                    ))}
                  </select>
                </div>

                <div className={styles.field}>
                  <label>Publication Year</label>

                  <input
                      type="number"
                      name="publicationYear"
                      value={form.publicationYear}
                      onChange={handleChange}
                  />
                </div>

                <div className={styles.field}>
                  <label>Price</label>

                  <input
                      type="number"
                      step="0.01"
                      name="price"
                      value={form.price}
                      onChange={handleChange}
                      required
                  />
                </div>

                <div className={styles.fieldFull}>
                  <label>Description</label>

                  <textarea
                      name="description"
                      value={form.description}
                      onChange={handleChange}
                      rows="4"
                  />
                </div>
              </div>

              <div className={styles.formActions}>
                <button
                    type="submit"
                    className={styles.primaryButton}
                >
                  Save Changes
                </button>

                <button
                    type="button"
                    onClick={resetForm}
                    className={styles.secondaryButton}
                >
                  Cancel
                </button>
              </div>
            </form>
        )}

        {/* =========================
          BOOK LIST
          ========================= */}

        <div className={styles.bookGrid}>
          {filteredBooks.length === 0 ? (
              <div className={styles.emptyState}>
                <h2>No books found</h2>

                <p>
                  {searchTerm || selectedCategory
                      ? "Try changing your search or category filter."
                      : "There are currently no available books."}
                </p>
              </div>
          ) : (
              filteredBooks.map((book) => {
                const bookId = getBookId(book);

                return (
                    <div
                        key={bookId}
                        className={styles.bookCard}
                    >
                      <div className={styles.bookTop}>
                        <div>
                          <h2 className={styles.bookTitle}>
                            {book.title}
                          </h2>

                          <p className={styles.author}>
                            by {book.author}
                          </p>
                        </div>

                        <span
                            className={styles.categoryBadge}
                        >
                    {getCategoryName(book)}
                  </span>
                      </div>

                      <div className={styles.bookDetails}>
                        <div>
                    <span
                        className={styles.detailLabel}
                    >
                      ISBN
                    </span>

                          <span>
                      {book.isbn}
                    </span>
                        </div>

                        <div>
                    <span
                        className={styles.detailLabel}
                    >
                      Published
                    </span>

                          <span>
                      {book.publicationYear ??
                          "N/A"}
                    </span>
                        </div>

                        <div>
                    <span
                        className={styles.detailLabel}
                    >
                      Price
                    </span>

                          <span className={styles.price}>
                      {getPrice(book)} МКД
                    </span>
                        </div>
                      </div>

                      {book.description && (
                          <p className={styles.description}>
                            {book.description}
                          </p>
                      )}

                      <div className={styles.cardActions}>
                        <button
                            type="button"
                            onClick={() =>
                                navigate(`/books/${encodeURIComponent(bookId)}/availability`)
                            }
                            className={
                              styles.availabilityButton
                            }
                        >
                          See Availability
                        </button>

                        <button
                            type="button"
                            onClick={() =>
                                startEditing(book)
                            }
                            className={styles.editButton}
                        >
                          Edit
                        </button>

                        <button
                            type="button"
                            onClick={() =>
                                handleDelete(book)
                            }
                            className={
                              styles.deleteButton
                            }
                        >
                          Delete
                        </button>
                      </div>
                    </div>
                );
              })
          )}
        </div>
      </div>
  );
}

export default BooksPage;
