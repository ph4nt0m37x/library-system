import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import keycloak from "../keycloak";

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
                    "http://localhost:8000/api/members/all",
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
            member.firstName.toLowerCase().includes(query) ||
            member.lastName.toLowerCase().includes(query) ||
            member.membershipNumber.toLowerCase().includes(query) ||
            member.email.toLowerCase().includes(query)
        );
    });

    if (loading) {
        return <div>Loading members...</div>;
    }

    if (error) {
        return <div>{error}</div>;
    }

    return (
        <div>
            <div>
                <h1>Members</h1>

                <Link to="/members/register">
                    <button>Register Member</button>
                </Link>
            </div>

            <div>
                <input
                    type="text"
                    placeholder="Search members..."
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                />
            </div>

            {filteredMembers.length === 0 ? (
                <p>No members found.</p>
            ) : (
                <div>
                    {filteredMembers.map((member) => (
                        <div key={member.memberId}>
                            <h2>
                                {member.firstName} {member.lastName}
                            </h2>

                            <p>
                                Membership number:{" "}
                                {member.membershipNumber}
                            </p>

                            <p>{member.email}</p>

                            <p>{member.phoneNumber}</p>

                            <p>
                                Member status:{" "}
                                {member.active ? "Active" : "Inactive"}
                            </p>

                            <p>
                                Subscription:{" "}
                                {member.currentSubscription
                                    ? `${member.currentSubscription.tier} - ${
  member.currentSubscription.paymentStatus ||
  "Unknown"
}`
                                    : "No active subscription"}
                            </p>

                            <Link to={`/members/${member.memberId}`}>
                                <button>View Details</button>
                            </Link>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

export default MembersPage;
