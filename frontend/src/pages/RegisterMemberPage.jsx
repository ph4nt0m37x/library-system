import { useState } from "react";
import { useNavigate } from "react-router-dom";
import keycloak from "../keycloak";
import styles from "../styles/RegisterMemberPage.module.css";

const API = "http://localhost:8000";

function RegisterMemberPage() {
    const navigate = useNavigate();

    const [form, setForm] = useState({
        firstName: "",
        lastName: "",
        email: "",
        phoneNumber: "",
    });

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const handleChange = (e) => {
        setForm({
            ...form,
            [e.target.name]: e.target.value,
        });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        try {
            setLoading(true);
            setError("");

            const response = await fetch(
                `${API}/api/members/register`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${keycloak.token}`,
                    },
                    body: JSON.stringify(form),
                }
            );

            if (!response.ok) {
                throw new Error("Failed to register member.");
            }

            const data = await response.json();

            // Backend returns { id: "..." }
            navigate(`/members/${data.id}`);
        } catch (err) {
            console.error(err);
            setError(err.message || "Failed to register member.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className={styles.page}>
            <div className={styles.header}>
                <h1 className={styles.title}>Register Member</h1>

                <p className={styles.subtitle}>
                    Add a new member to the library system.
                </p>
            </div>

            {error && <div className={styles.error}>{error}</div>}

            <form
                className={styles.formCard}
                onSubmit={handleSubmit}
            >
                <div className={styles.formGrid}>
                    <div className={styles.field}>
                        <label htmlFor="firstName">
                            First name
                        </label>
                        <input
                            id="firstName"
                            name="firstName"
                            type="text"
                            value={form.firstName}
                            onChange={handleChange}
                            placeholder="Jane"
                            required
                        />
                    </div>

                    <div className={styles.field}>
                        <label htmlFor="lastName">
                            Last name
                        </label>
                        <input
                            id="lastName"
                            name="lastName"
                            type="text"
                            value={form.lastName}
                            onChange={handleChange}
                            placeholder="Doe"
                            required
                        />
                    </div>

                    <div className={styles.field}>
                        <label htmlFor="email">Email</label>
                        <input
                            id="email"
                            name="email"
                            type="email"
                            value={form.email}
                            onChange={handleChange}
                            placeholder="jane.doe@example.com"
                            required
                        />
                    </div>

                    <div className={styles.field}>
                        <label htmlFor="phoneNumber">
                            Phone number
                        </label>
                        <input
                            id="phoneNumber"
                            name="phoneNumber"
                            type="text"
                            value={form.phoneNumber}
                            onChange={handleChange}
                            placeholder=" 070 123 456"
                            required
                        />
                    </div>
                </div>

                <div className={styles.formActions}>
                    <button
                        type="button"
                        className={styles.secondaryButton}
                        onClick={() => navigate("/members")}
                        disabled={loading}
                    >
                        Cancel
                    </button>

                    <button
                        type="submit"
                        className={styles.primaryButton}
                        disabled={loading}
                    >
                        {loading
                            ? "Registering..."
                            : "Register Member"}
                    </button>
                </div>
            </form>
        </div>
    );
}

export default RegisterMemberPage;