import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import keycloak from "../keycloak";
import styles from "../styles/MembersPage.module.css";

const API = "http://localhost:8000";

const prettyEnum = (value) => {
    if (!value) return "";

    return String(value)
        .toLowerCase()
        .split("_")
        .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
        .join(" ");
};

function MembersPage() {
    const [members, setMembers] = useState([]);
    const [search, setSearch] = useState("");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const fetchMembers = async () => {
            try {
                setLoading(true);
                setError("");

                const response = await fetch(
                    `${API}/api/members/all`,
                    {
                        headers: {
                            Authorization: `Bearer ${keycloak.token}`,
                        },
                    }
                );

                if (!response.ok) {
                    throw new Error("Failed to load members.");
                }

                const data = await response.json();
                setMembers(data);
            } catch (err) {
                console.error(err);
                setError(err.message || "Failed to load members.");
            } finally {
                setLoading(false);
            }
        };

        fetchMembers();
    }, []);

    const filteredMembers = members.filter((member) => {
        const query = search.toLowerCase();

        return (
            member.firstName?.toLowerCase().includes(query) ||
            member.lastName?.toLowerCase().includes(query) ||
            member.membershipNumber?.toLowerCase().includes(query) ||
            member.email?.toLowerCase().includes(query)
        );
    });

    if (loading) {
        return (
            <div className={styles.page}>
                <div className={styles.emptyState}>
                    <h2>Loading members…</h2>
                </div>
            </div>
        );
    }

    return (
        <div className={styles.page}>
            {/* HEADER */}

            <div className={styles.header}>
                <div>
                    <h1 className={styles.title}>Members</h1>

                    <p className={styles.subtitle}>
                        Browse and manage library members.
                    </p>
                </div>

                <Link
                    to="/members/register"
                    className={styles.primaryButton}
                >
                    + Register Member
                </Link>
            </div>

            {error && <div className={styles.error}>{error}</div>}

            {/* SEARCH */}

            <div className={styles.searchSection}>
                <input
                    type="text"
                    className={styles.searchInput}
                    placeholder="Search by name, membership number, or email..."
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                />

                {search && (
                    <p className={styles.resultText}>
                        Showing {filteredMembers.length}{" "}
                        {filteredMembers.length === 1
                            ? "member"
                            : "members"}
                    </p>
                )}
            </div>

            {/* MEMBER LIST */}

            {filteredMembers.length === 0 ? (
                <div className={styles.emptyState}>
                    <h2>No members found</h2>
                    <p>
                        {search
                            ? "Try changing your search."
                            : "No members have registered yet."}
                    </p>
                </div>
            ) : (
                <div className={styles.grid}>
                    {filteredMembers.map((member) => (
                        <div
                            key={member.memberId}
                            className={styles.card}
                        >
                            <div className={styles.cardHeader}>
                                <div>
                                    <h2 className={styles.cardTitle}>
                                        {member.firstName}{" "}
                                        {member.lastName}
                                    </h2>

                                    <span
                                        className={styles.cardSubtitle}
                                    >
                                        #{member.membershipNumber}
                                    </span>
                                </div>

                                <span
                                    className={`${styles.status} ${
                                        member.active
                                            ? styles.statusActive
                                            : styles.statusInactive
                                    }`}
                                >
                                    {member.active
                                        ? "Active"
                                        : "Inactive"}
                                </span>
                            </div>

                            <div className={styles.details}>
                                <div className={styles.detailRow}>
                                    <span
                                        className={styles.detailLabel}
                                    >
                                        Email
                                    </span>
                                    <span
                                        className={styles.detailValue}
                                    >
                                        {member.email ?? "—"}
                                    </span>
                                </div>

                                <div className={styles.detailRow}>
                                    <span
                                        className={styles.detailLabel}
                                    >
                                        Phone
                                    </span>
                                    <span
                                        className={styles.detailValue}
                                    >
                                        {member.phoneNumber ?? "—"}
                                    </span>
                                </div>

                                <div className={styles.subscription}>
                                    <span
                                        className={styles.detailLabel}
                                    >
                                        Subscription
                                    </span>

                                    {member.currentSubscription ? (
                                        <span
                                            className={
                                                styles.subscriptionBadge
                                            }
                                        >
                                            {prettyEnum(
                                                member
                                                    .currentSubscription
                                                    .tier
                                            )}
                                            {" · "}
                                            {prettyEnum(
                                                member
                                                    .currentSubscription
                                                    .paymentStatus
                                            ) || "Unknown"}
                                        </span>
                                    ) : (
                                        <span
                                            className={
                                                styles.subscriptionMuted
                                            }
                                        >
                                            No active subscription
                                        </span>
                                    )}
                                </div>
                            </div>

                            <div className={styles.cardActions}>
                                <Link
                                    to={`/members/${member.memberId}`}
                                    className={styles.secondaryButton}
                                >
                                    View Details
                                </Link>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

export default MembersPage;