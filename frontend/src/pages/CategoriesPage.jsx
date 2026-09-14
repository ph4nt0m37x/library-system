import { useEffect, useState } from "react";
import keycloak from "../keycloak";
import styles from "../styles/CategoriesPage.module.css";

function CategoriesPage() {
    const [categories, setCategories] = useState([]);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const [showForm, setShowForm] = useState(false);
    const [editingCategory, setEditingCategory] = useState(null);

    const [name, setName] = useState("");

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
            setError("Could not load categories.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadCategories();
    }, []);

    // =========================
    // CREATE
    // =========================

    const handleCreate = async (event) => {
        event.preventDefault();

        try {
            const response = await fetch(
                "http://localhost:8000/api/categories/create",
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${keycloak.token}`,
                    },
                    body: JSON.stringify({
                        name: name.trim(),
                    }),
                }
            );

            if (!response.ok) {
                const errorText = await response.text();

                console.error(
                    "Create category error:",
                    response.status,
                    errorText
                );

                throw new Error(
                    `Failed to create category: ${response.status}`
                );
            }

            resetForm();
            await loadCategories();
        } catch (error) {
            console.error(error);
            setError("Could not create category.");
        }
    };

    // =========================
    // START UPDATE
    // =========================

    const startEditing = (category) => {
        setEditingCategory(category);
        setName(category.name);
        setShowForm(false);
        setError("");
    };

    // =========================
    // UPDATE
    // =========================

    const handleUpdate = async (event) => {
        event.preventDefault();

        try {
            const response = await fetch(
                `http://localhost:8000/api/categories/update/${editingCategory.id}`,
                {
                    method: "PUT",
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${keycloak.token}`,
                    },
                    body: JSON.stringify({
                        name: name.trim(),
                    }),
                }
            );

            if (!response.ok) {
                const errorText = await response.text();

                console.error(
                    "Update category error:",
                    response.status,
                    errorText
                );

                throw new Error(
                    `Failed to update category: ${response.status}`
                );
            }

            resetForm();
            await loadCategories();
        } catch (error) {
            console.error(error);
            setError("Could not update category.");
        }
    };

    // =========================
    // DELETE
    // =========================

    const handleDelete = async (category) => {
        const confirmed = window.confirm(
            `Are you sure you want to delete "${category.name}"?`
        );

        if (!confirmed) {
            return;
        }

        try {
            const response = await fetch(
                `http://localhost:8000/api/categories/delete/${category.id}`,
                {
                    method: "DELETE",
                    headers: {
                        Authorization: `Bearer ${keycloak.token}`,
                    },
                }
            );

            if (!response.ok) {
                const errorText = await response.text();

                console.error(
                    "Delete category error:",
                    response.status,
                    errorText
                );

                throw new Error(
                    `Failed to delete category: ${response.status}`
                );
            }

            await loadCategories();
        } catch (error) {
            console.error(error);
            setError("Could not delete category.");
        }
    };

    // =========================
    // RESET
    // =========================

    const resetForm = () => {
        setName("");
        setShowForm(false);
        setEditingCategory(null);
        setError("");
    };

    // =========================
    // LOADING
    // =========================

    if (loading) {
        return (
            <div className={styles.page}>
                <p>Loading categories...</p>
            </div>
        );
    }

    // =========================
    // PAGE
    // =========================

    return (
        <div className={styles.page}>
            {/* HEADER */}

            <div className={styles.header}>
                <div>
                    <h1 className={styles.title}>Categories</h1>

                    <p className={styles.subtitle}>
                        Manage the categories used by the book catalogue.
                    </p>
                </div>

                {!editingCategory && (
                    <button
                        className={styles.primaryButton}
                        onClick={() => {
                            setShowForm(!showForm);
                            setError("");
                        }}
                    >
                        {showForm ? "Cancel" : "+ Add Category"}
                    </button>
                )}
            </div>

            {error && (
                <div className={styles.error}>{error}</div>
            )}

            {/* CREATE FORM */}

            {showForm && !editingCategory && (
                <form
                    onSubmit={handleCreate}
                    className={styles.formCard}
                >
                    <h2 className={styles.formTitle}>
                        Add Category
                    </h2>

                    <div className={styles.field}>
                        <label>Category Name</label>

                        <input
                            value={name}
                            onChange={(event) =>
                                setName(event.target.value)
                            }
                            placeholder="e.g. Fantasy"
                            required
                        />
                    </div>

                    <div className={styles.formActions}>
                        <button
                            type="submit"
                            className={styles.primaryButton}
                        >
                            Add Category
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

            {/* UPDATE FORM */}

            {editingCategory && (
                <form
                    onSubmit={handleUpdate}
                    className={styles.formCard}
                >
                    <h2 className={styles.formTitle}>
                        Update Category
                    </h2>

                    <div className={styles.field}>
                        <label>Category Name</label>

                        <input
                            value={name}
                            onChange={(event) =>
                                setName(event.target.value)
                            }
                            required
                        />
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

            {/* CATEGORY LIST */}

            {categories.length === 0 ? (
                <div className={styles.emptyState}>
                    <h2>No categories found</h2>

                    <p>Add your first category above.</p>
                </div>
            ) : (
                <div className={styles.categoryGrid}>
                    {categories.map((category) => (
                        <div
                            key={category.id}
                            className={styles.categoryCard}
                        >
                            <h2 className={styles.categoryName}>
                                {category.name}
                            </h2>

                            <div className={styles.actions}>
                                <button
                                    onClick={() =>
                                        startEditing(category)
                                    }
                                    className={styles.editButton}
                                >
                                    Edit
                                </button>

                                <button
                                    onClick={() =>
                                        handleDelete(category)
                                    }
                                    className={styles.deleteButton}
                                >
                                    Delete
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

export default CategoriesPage;