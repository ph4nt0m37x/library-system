import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
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

    useEffect(() => {
        const loadAvailability = async () => {
            try {
                const headers = { Authorization: `Bearer ${keycloak.token}` };
                const [bookResponse, librariesResponse, stockResponse] = await Promise.all([
                    fetch(`${API}/api/books/${encodeURIComponent(id)}`, { headers }),
                    fetch(`${API}/api/libraries/available`, { headers }),
                    fetch(`${API}/api/stock/book/${encodeURIComponent(id)}`, { headers }),
                ]);

                if (!bookResponse.ok) throw new Error("Book not found.");
                if (!librariesResponse.ok || !stockResponse.ok) {
                    throw new Error("Could not load book availability.");
                }

                const [bookData, libraries, stock] = await Promise.all([
                    bookResponse.json(),
                    librariesResponse.json(),
                    stockResponse.json(),
                ]);
                const libraryNames = new Map(
                    libraries.map((library) => [String(getId(library.id)), library.name])
                );

                setBook(bookData);
                setAvailability(stock.map((item) => ({
                    ...item,
                    libraryName: libraryNames.get(String(getId(item.libraryId))) ?? getId(item.libraryId),
                })));
            } catch (err) {
                setError(err.message || "Could not load book availability.");
            } finally {
                setLoading(false);
            }
        };

        loadAvailability();
    }, [id]);

    if (loading) return <div className={styles.page}>Loading availability...</div>;

    return (
        <div className={styles.page}>
            <button className={styles.backButton} onClick={() => navigate(-1)}>
                Back
            </button>

            {error ? (
                <p className={styles.error}>{error}</p>
            ) : (
                <>
                    <h1>{book?.title}</h1>
                    <p className={styles.subtitle}>Availability by library</p>

                    {availability.length === 0 ? (
                        <p>This book is not currently stocked in any library.</p>
                    ) : (
                        <table className={styles.table}>
                            <thead>
                                <tr>
                                    <th>Library</th>
                                    <th>Total copies</th>
                                    <th>Available</th>
                                    <th>Borrowed</th>
                                </tr>
                            </thead>
                            <tbody>
                                {availability.map((item) => (
                                    <tr key={getId(item.libraryId)}>
                                        <td>{item.libraryName}</td>
                                        <td>{item.totalQuantity}</td>
                                        <td>{item.availableQuantity}</td>
                                        <td>{item.borrowedQuantity}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                </>
            )}
        </div>
    );
}

export default BookAvailabilityPage;
