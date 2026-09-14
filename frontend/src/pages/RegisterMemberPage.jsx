import { useState } from "react";
import { useNavigate } from "react-router-dom";
import keycloak from "../keycloak";

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
                "http://localhost:8000/api/members/register",
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
        <div>
            <h1>Register Member</h1>

            {error && <p>{error}</p>}

            <form onSubmit={handleSubmit}>
                <div>
                    <label htmlFor="firstName">First name</label>
                    <input
                        id="firstName"
                        name="firstName"
                        type="text"
                        value={form.firstName}
                        onChange={handleChange}
                        required
                    />
                </div>

                <div>
                    <label htmlFor="lastName">Last name</label>
                    <input
                        id="lastName"
                        name="lastName"
                        type="text"
                        value={form.lastName}
                        onChange={handleChange}
                        required
                    />
                </div>

                <div>
                    <label htmlFor="email">Email</label>
                    <input
                        id="email"
                        name="email"
                        type="email"
                        value={form.email}
                        onChange={handleChange}
                        required
                    />
                </div>

                <div>
                    <label htmlFor="phoneNumber">Phone number</label>
                    <input
                        id="phoneNumber"
                        name="phoneNumber"
                        type="text"
                        value={form.phoneNumber}
                        onChange={handleChange}
                        required
                    />
                </div>

                <button type="submit" disabled={loading}>
                    {loading ? "Registering..." : "Register Member"}
                </button>

                <button
                    type="button"
                    onClick={() => navigate("/members")}
                    disabled={loading}
                >
                    Cancel
                </button>
            </form>
        </div>
    );
}

export default RegisterMemberPage;
