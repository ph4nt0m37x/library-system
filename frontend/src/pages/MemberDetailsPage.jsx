import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
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

    // Subscription modal
    const [showSubscriptionModal, setShowSubscriptionModal] =
        useState(false);

    const [subscriptionForm, setSubscriptionForm] = useState({
        tier: "THREE_MONTHS",
        startsAt: "",
    });

    const [startingSubscription, setStartingSubscription] =
        useState(false);

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

// -----------------------------
// Member editing
// -----------------------------

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
                const message =
                    await nameResponse.text();

                throw new Error(
                    message ||
                    "Failed to update member name."
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
                const message =
                    await contactResponse.text();

                throw new Error(
                    message ||
                    "Failed to update contact details."
                );
            }
        }

        // Axon read model may need a moment to update
        let updatedMember = null;

        for (let attempt = 0; attempt < 5; attempt++) {
            await new Promise((resolve) =>
                setTimeout(resolve, 300)
            );

            updatedMember = await fetchMember();

            const updated =
                updatedMember.firstName ===
                form.firstName &&
                updatedMember.lastName ===
                form.lastName &&
                updatedMember.email === form.email &&
                updatedMember.phoneNumber ===
                form.phoneNumber;

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

// -----------------------------
// Subscription
// -----------------------------

const handleSubscriptionChange = (e) => {
    const { name, value } = e.target;

    setSubscriptionForm((previous) => ({
        ...previous,
        [name]: value,
    }));
};

const openSubscriptionModal = () => {
    setError("");

    setSubscriptionForm({
        tier: "THREE_MONTHS",
        startsAt: "",
    });

    setShowSubscriptionModal(true);
};

const closeSubscriptionModal = () => {
    if (startingSubscription) {
        return;
    }

    setShowSubscriptionModal(false);

    setSubscriptionForm({
        tier: "THREE_MONTHS",
        startsAt: "",
    });
};

const handleStartSubscription = async () => {
    if (
        !subscriptionForm.tier ||
        !subscriptionForm.startsAt
    ) {
        setError(
            "Please select a tier and start date."
        );

        return;
    }

    try {
        setStartingSubscription(true);
        setError("");

        const response = await fetch(
            `http://localhost:8000/api/members/${member.memberId}/subscriptions/start`,
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${keycloak.token}`,
                },
                body: JSON.stringify({
                    tier: subscriptionForm.tier,
                    startsAt: subscriptionForm.startsAt,
                }),
            }
        );

        if (!response.ok) {
            const message =
                await response.text();

            throw new Error(
                message ||
                "Failed to start subscription."
            );
        }

        setShowSubscriptionModal(false);

        setSubscriptionForm({
            tier: "THREE_MONTHS",
            startsAt: "",
        });

        /*
         * The command succeeded, but the Axon read model
         * may need a moment to update.
         */
        for (let attempt = 0; attempt < 5; attempt++) {
            await new Promise((resolve) =>
                setTimeout(resolve, 300)
            );

            const updatedMember =
                await fetchMember();

            if (
                updatedMember.currentSubscription
            ) {
                break;
            }
        }
    } catch (err) {
        console.error(err);

        setError(
            err.message ||
            "Failed to start subscription."
        );
    } finally {
        setStartingSubscription(false);
    }
};

// -----------------------------
// Loading / not found
// -----------------------------

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

        {/* -------------------------------- */}
        {/* Personal Information */}
        {/* -------------------------------- */}

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

        {/* -------------------------------- */}
        {/* Membership Information */}
        {/* -------------------------------- */}

        <section>
            <h2>Membership Information</h2>

            <p>
                <strong>Membership number:</strong>{" "}
                {member.membershipNumber}
            </p>

            <p>
                <strong>Status:</strong>{" "}
                {member.active
                    ? "Active"
                    : "Inactive"}
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

        {/* -------------------------------- */}
        {/* Current Subscription */}
        {/* -------------------------------- */}

        <section>
            <h2>Current Subscription</h2>

            {member.currentSubscription ? (
                <div>
                    <p>
                        <strong>Tier:</strong>{" "}
                        {
                            member.currentSubscription
                                .tier
                        }
                    </p>

                    <p>
                        <strong>
                            Payment status:
                        </strong>{" "}
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
                                member
                                    .currentSubscription
                                    .paymentReference
                            }
                        </p>
                    )}
                </div>
            ) : (
                <p>No active subscription.</p>
            )}
        </section>

        {/* -------------------------------- */}
        {/* Subscription History */}
        {/* -------------------------------- */}

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
                                <strong>
                                    Tier:
                                </strong>{" "}
                                {subscription.tier}
                            </p>

                            <p>
                                <strong>
                                    Starts:
                                </strong>{" "}
                                {new Date(
                                    subscription.startsAt
                                ).toLocaleString()}
                            </p>

                            <p>
                                <strong>
                                    Ends:
                                </strong>{" "}
                                {new Date(
                                    subscription.endsAt
                                ).toLocaleString()}
                            </p>

                            <p>
                                <strong>
                                    Amount:
                                </strong>{" "}
                                {
                                    subscription.amountPaid
                                }{" "}
                                {
                                    subscription.currency
                                }
                            </p>

                            <hr />
                        </div>
                    )
                )
            ) : (
                <p>
                    No subscription history.
                </p>
            )}
        </section>

        {/* -------------------------------- */}
        {/* Subscription Actions */}
        {/* -------------------------------- */}

        <section>
            <h2>Subscription Actions</h2>

            <button
                type="button"
                onClick={openSubscriptionModal}
            >
                Start Subscription
            </button>
        </section>

        {/* -------------------------------- */}
        {/* Start Subscription Modal */}
        {/* -------------------------------- */}

        {showSubscriptionModal && (
            <div
                style={{
                    position: "fixed",
                    inset: 0,
                    backgroundColor:
                        "rgba(0, 0, 0, 0.5)",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    zIndex: 1000,
                }}
                onClick={closeSubscriptionModal}
            >
                <div
                    style={{
                        backgroundColor: "white",
                        padding: "24px",
                        borderRadius: "8px",
                        minWidth: "400px",
                        maxWidth: "90%",
                    }}
                    onClick={(e) =>
                        e.stopPropagation()
                    }
                >
                    <h2>Start Subscription</h2>

                    {/* Tier */}

                    <div>
                        <label htmlFor="tier">
                            Tier
                        </label>

                        <select
                            id="tier"
                            name="tier"
                            value={
                                subscriptionForm.tier
                            }
                            onChange={
                                handleSubscriptionChange
                            }
                            disabled={
                                startingSubscription
                            }
                        >
                            <option value="THREE_MONTHS">
                                3 Months
                            </option>

                            <option value="SIX_MONTHS">
                                6 Months
                            </option>

                            <option value="TWELVE_MONTHS">
                                12 Months
                            </option>
                        </select>
                    </div>

                    {/* Starts At */}

                    <div>
                        <label htmlFor="startsAt">
                            Starts at
                        </label>

                        <input
                            id="startsAt"
                            name="startsAt"
                            type="datetime-local"
                            value={
                                subscriptionForm.startsAt
                            }
                            onChange={
                                handleSubscriptionChange
                            }
                            disabled={
                                startingSubscription
                            }
                            required
                        />
                    </div>

                    {/* Buttons */}

                    <div>
                        <button
                            type="button"
                            onClick={
                                closeSubscriptionModal
                            }
                            disabled={
                                startingSubscription
                            }
                        >
                            Cancel
                        </button>

                        <button
                            type="button"
                            onClick={
                                handleStartSubscription
                            }
                            disabled={
                                startingSubscription
                            }
                        >
                            {startingSubscription
                                ? "Starting..."
                                : "Start Subscription"}
                        </button>
                    </div>
                </div>
            </div>
        )}
    </div>
);
}

export default MemberDetailsPage;
