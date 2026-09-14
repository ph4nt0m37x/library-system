import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import keycloak from "../keycloak";

function MemberDetailsPage() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [member, setMember] = useState(null);

    const [form, setForm] = useState({
        firstName: "",
        lastName: "",
        email: "",
        phoneNumber: "",
    });

    const [editing, setEditing] = useState(false);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");

    const fetchMember = async () => {
        const response = await fetch(
            `http://localhost:8000/api/members/${id}`,
{
    headers: {
        Authorization: `Bearer ${keycloak.token}`,
    },
}
);

if (!response.ok) {
    throw new Error("Failed to load member.");
}

const data = await response.json();

setMember(data);

setForm({
    firstName: data.firstName,
    lastName: data.lastName,
    email: data.email,
    phoneNumber: data.phoneNumber,
});

return data;
};

useEffect(() => {
    const loadMember = async () => {
        try {
            setLoading(true);
            setError("");

            await fetchMember();
        } catch (err) {
            console.error(err);
            setError(
                err.message || "Failed to load member."
            );
        } finally {
            setLoading(false);
        }
    };

    loadMember();
}, [id]);

const handleChange = (e) => {
    const { name, value } = e.target;

    setForm((previous) => ({
        ...previous,
        [name]: value,
    }));
};

const handleEdit = () => {
    setForm({
        firstName: member.firstName,
        lastName: member.lastName,
        email: member.email,
        phoneNumber: member.phoneNumber,
    });

    setError("");
    setEditing(true);
};

const handleCancel = () => {
    setForm({
        firstName: member.firstName,
        lastName: member.lastName,
        email: member.email,
        phoneNumber: member.phoneNumber,
    });

    setError("");
    setEditing(false);
};

const handleSave = async () => {
    try {
        setSaving(true);
        setError("");

        const nameChanged =
            form.firstName !== member.firstName ||
            form.lastName !== member.lastName;

        const contactChanged =
            form.email !== member.email ||
            form.phoneNumber !== member.phoneNumber;

        if (!nameChanged && !contactChanged) {
            setEditing(false);
            return;
        }

        if (nameChanged) {
            const nameResponse = await fetch(
                `http://localhost:8000/api/members/${id}/name`,
                {
                    method: "PUT",
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${keycloak.token}`,
                    },
                    body: JSON.stringify({
                        firstName: form.firstName,
                        lastName: form.lastName,
                    }),
                }
            );

            if (!nameResponse.ok) {
                const message = await nameResponse.text();

                throw new Error(
                    message || "Failed to update member name."
                );
            }
        }

        if (contactChanged) {
            const contactResponse = await fetch(
                `http://localhost:8000/api/members/${id}/contact-details`,
                {
                    method: "PUT",
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${keycloak.token}`,
                    },
                    body: JSON.stringify({
                        email: form.email,
                        phoneNumber: form.phoneNumber,
                    }),
                }
            );

            if (!contactResponse.ok) {
                const message = await contactResponse.text();

                throw new Error(
                    message ||
                    "Failed to update contact details."
                );
            }
        }

        /*
         * The command succeeded, but the Axon read model
         * may need a moment to update.
         *
         * Reload a few times until the new values appear.
         */
        let updatedMember = null;

        for (let attempt = 0; attempt < 5; attempt++) {
            await new Promise((resolve) =>
                setTimeout(resolve, 300)
            );

            updatedMember = await fetchMember();

            const updated =
                updatedMember.firstName === form.firstName &&
                updatedMember.lastName === form.lastName &&
                updatedMember.email === form.email &&
                updatedMember.phoneNumber === form.phoneNumber;

            if (updated) {
                break;
            }
        }

        setEditing(false);
    } catch (err) {
        console.error(err);

        setError(
            err.message || "Failed to update member."
        );
    } finally {
        setSaving(false);
    }
};

if (loading) {
    return <p>Loading member...</p>;
}

if (!member) {
    return (
        <div>
            <p>{error || "Member not found."}</p>

            <button
                type="button"
                onClick={() => navigate("/members")}
            >
                Back to Members
            </button>
        </div>
    );
}

return (
    <div>
        <button
            type="button"
            onClick={() => navigate("/members")}
        >
            ← Back to Members
        </button>

        <h1>Member Details</h1>

        {error && <p>{error}</p>}

        <section>
            <h2>Personal Information</h2>

            {editing ? (
                <>
                    <div>
                        <label htmlFor="firstName">
                            First name
                        </label>

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
                        <label htmlFor="lastName">
                            Last name
                        </label>

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
                        <label htmlFor="email">
                            Email
                        </label>

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
                        <label htmlFor="phoneNumber">
                            Phone number
                        </label>

                        <input
                            id="phoneNumber"
                            name="phoneNumber"
                            type="text"
                            value={form.phoneNumber}
                            onChange={handleChange}
                            required
                        />
                    </div>

                    <button
                        type="button"
                        onClick={handleCancel}
                        disabled={saving}
                    >
                        Cancel
                    </button>

                    <button
                        type="button"
                        onClick={handleSave}
                        disabled={saving}
                    >
                        {saving
                            ? "Saving..."
                            : "Save Changes"}
                    </button>
                </>
            ) : (
                <>
                    <p>
                        <strong>Name:</strong>{" "}
                        {member.firstName}{" "}
                        {member.lastName}
                    </p>

                    <p>
                        <strong>Email:</strong>{" "}
                        {member.email}
                    </p>

                    <p>
                        <strong>Phone:</strong>{" "}
                        {member.phoneNumber}
                    </p>

                    <button
                        type="button"
                        onClick={handleEdit}
                    >
                        Edit
                    </button>
                </>
            )}
        </section>

        <section>
            <h2>Membership Information</h2>

            <p>
                <strong>Membership number:</strong>{" "}
                {member.membershipNumber}
            </p>

            <p>
                <strong>Status:</strong>{" "}
                {member.active ? "Active" : "Inactive"}
            </p>

            <p>
                <strong>Registered:</strong>{" "}
                {new Date(
                    member.registeredAt
                ).toLocaleString()}
            </p>

            {member.changedAt && (
                <p>
                    <strong>Last changed:</strong>{" "}
                    {new Date(
                        member.changedAt
                    ).toLocaleString()}
                </p>
            )}
        </section>

        <section>
            <h2>Current Subscription</h2>

            {member.currentSubscription ? (
                <div>
                    <p>
                        <strong>Tier:</strong>{" "}
                        {member.currentSubscription.tier}
                    </p>

                    <p>
                        <strong>Payment status:</strong>{" "}
                        {
                            member.currentSubscription
                                .paymentStatus
                        }
                    </p>

                    <p>
                        <strong>Starts:</strong>{" "}
                        {new Date(
                            member.currentSubscription.startsAt
                        ).toLocaleString()}
                    </p>

                    <p>
                        <strong>Ends:</strong>{" "}
                        {new Date(
                            member.currentSubscription.endsAt
                        ).toLocaleString()}
                    </p>

                    <p>
                        <strong>Amount paid:</strong>{" "}
                        {
                            member.currentSubscription
                                .amountPaid
                        }{" "}
                        {
                            member.currentSubscription
                                .currency
                        }
                    </p>

                    <p>
                        <strong>Paid at:</strong>{" "}
                        {new Date(
                            member.currentSubscription.paidAt
                        ).toLocaleString()}
                    </p>

                    {member.currentSubscription
                        .paymentReference && (
                        <p>
                            <strong>
                                Payment reference:
                            </strong>{" "}
                            {
                                member.currentSubscription
                                    .paymentReference
                            }
                        </p>
                    )}
                </div>
            ) : (
                <p>No active subscription.</p>
            )}
        </section>

        <section>
            <h2>Subscription History</h2>

            {member.subscriptionHistory &&
            member.subscriptionHistory.length > 0 ? (
                member.subscriptionHistory.map(
                    (subscription) => (
                        <div
                            key={
                                subscription.subscriptionId
                            }
                        >
                            <p>
                                <strong>Tier:</strong>{" "}
                                {subscription.tier}
                            </p>

                            <p>
                                <strong>Starts:</strong>{" "}
                                {new Date(
                                    subscription.startsAt
                                ).toLocaleString()}
                            </p>

                            <p>
                                <strong>Ends:</strong>{" "}
                                {new Date(
                                    subscription.endsAt
                                ).toLocaleString()}
                            </p>

                            <p>
                                <strong>Amount:</strong>{" "}
                                {
                                    subscription.amountPaid
                                }{" "}
                                {subscription.currency}
                            </p>

                            <hr />
                        </div>
                    )
                )
            ) : (
                <p>No subscription history.</p>
            )}
        </section>

        <section>
            <h2>Subscription Actions</h2>

            <Link
                to={`/members/${member.memberId}/subscriptions/start`}
            >
                Start Subscription
            </Link>

            {" "}

            <Link
                to={`/members/${member.memberId}/subscriptions/renew`}
            >
                Renew Subscription
            </Link>
        </section>
    </div>
);
}

export default MemberDetailsPage;