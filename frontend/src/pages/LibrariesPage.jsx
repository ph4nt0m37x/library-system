import { useEffect, useState } from "react";
import keycloak from "../keycloak";
import styles from "../styles/LibrariesPage.module.css";

function LibrariesPage() {
  const [libraries, setLibraries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [name, setName] = useState("");
  const [address, setAddress] = useState("");

  const [editingLibrary, setEditingLibrary] = useState(null);
  const [saving, setSaving] = useState(false);

  const getLibraryId = (library) => {
    return library.id?.value ?? library.id?.id ?? library.id;
  };

  const loadLibraries = async () => {
    try {
      setError("");

      const response = await fetch(
          "http://localhost:8000/api/libraries/available",
          {
            headers: {
              Authorization: `Bearer ${keycloak.token}`,
            },
          }
      );

      if (!response.ok) {
        throw new Error(`Failed to fetch libraries: ${response.status}`);
      }

      const data = await response.json();

      console.log("Libraries:", data);
      setLibraries(data);
    } catch (error) {
      console.error(error);
      setError("Could not load libraries.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (keycloak.token) {
      loadLibraries();
    } else {
      setError("You are not authenticated.");
      setLoading(false);
    }
  }, []);

  const clearForm = () => {
    setName("");
    setAddress("");
    setEditingLibrary(null);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!name.trim() || !address.trim()) {
      setError("Name and address are required.");
      return;
    }

    setSaving(true);
    setError("");

    try {
      let response;

      if (editingLibrary) {
        response = await fetch(
            "http://localhost:8000/api/libraries/update",
            {
              method: "PUT",
              headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${keycloak.token}`,
              },
              body: JSON.stringify({
                id: getLibraryId(editingLibrary),
                name: name.trim(),
                address: address.trim(),
              }),
            }
        );
      } else {
        response = await fetch(
            "http://localhost:8000/api/libraries/create",
            {
              method: "POST",
              headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${keycloak.token}`,
              },
              body: JSON.stringify({
                name: name.trim(),
                address: address.trim(),
              }),
            }
        );
      }

      if (!response.ok) {
        throw new Error(
            `Failed to ${editingLibrary ? "update" : "create"} library: ${response.status}`
        );
      }

      clearForm();
      await loadLibraries();
    } catch (error) {
      console.error(error);
      setError(
          editingLibrary
              ? "Could not update library."
              : "Could not create library."
      );
    } finally {
      setSaving(false);
    }
  };

  const handleEdit = (library) => {
    setEditingLibrary(library);
    setName(library.name ?? "");
    setAddress(library.address ?? "");
    setError("");
  };

  const handleDelete = async (library) => {
    const libraryId = getLibraryId(library);

    const confirmed = window.confirm(
        `Are you sure you want to delete "${library.name}"?`
    );

    if (!confirmed) {
      return;
    }

    try {
      setError("");

      const response = await fetch(
          "http://localhost:8000/api/libraries/delete",
          {
            method: "DELETE",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${keycloak.token}`,
            },
            body: JSON.stringify({
              id: libraryId,
            }),
          }
      );

      if (!response.ok) {
        throw new Error(`Failed to delete library: ${response.status}`);
      }

      if (editingLibrary && getLibraryId(editingLibrary) === libraryId) {
        clearForm();
      }

      await loadLibraries();
    } catch (error) {
      console.error(error);
      setError("Could not delete library.");
    }
  };

  if (loading) {
    return (
        <div className={styles.page}>
          <p>Loading libraries...</p>
        </div>
    );
  }

  return (
      <div className={styles.page}>
        {/* HEADER */}

        <div className={styles.header}>
          <div>
            <h1 className={styles.title}>Libraries</h1>

            <p className={styles.subtitle}>
              Manage the libraries that hold the book catalogue.
            </p>
          </div>
        </div>

        {error && <div className={styles.error}>{error}</div>}

        {/* FORM */}

        <div className={styles.formCard}>
          <h2 className={styles.formTitle}>
            {editingLibrary ? "Edit Library" : "Add Library"}
          </h2>

          <form onSubmit={handleSubmit}>
            <div className={styles.formGroup}>
              <label>Name</label>
              <input
                  type="text"
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  placeholder="Library name"
              />
            </div>

            <div className={styles.formGroup}>
              <label>Address</label>
              <input
                  type="text"
                  value={address}
                  onChange={(event) => setAddress(event.target.value)}
                  placeholder="Library address"
              />
            </div>

            <div className={styles.formActions}>
              <button
                  type="submit"
                  disabled={saving}
                  className={styles.primaryButton}
              >
                {saving
                    ? "Saving..."
                    : editingLibrary
                        ? "Update Library"
                        : "Add Library"}
              </button>

              {editingLibrary && (
                  <button
                      type="button"
                      onClick={clearForm}
                      className={styles.secondaryButton}
                  >
                    Cancel
                  </button>
              )}
            </div>
          </form>
        </div>

        {/* LIST */}

        <h2 className={styles.sectionTitle}>All Libraries</h2>

        {libraries.length === 0 ? (
            <div className={styles.emptyState}>
              <h2>No libraries found</h2>
              <p>Add your first library above.</p>
            </div>
        ) : (
            <div className={styles.libraryGrid}>
              {libraries.map((library) => (
                  <div
                      key={getLibraryId(library)}
                      className={styles.libraryCard}
                  >
                    <h2 className={styles.libraryName}>
                      {library.name}
                    </h2>

                    <p className={styles.libraryAddress}>
                      <strong>Address:</strong> {library.address}
                    </p>

                    <div className={styles.cardActions}>
                      <button onClick={() => handleEdit(library)}>
                        Edit
                      </button>

                      <button onClick={() => handleDelete(library)}>
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

export default LibrariesPage;