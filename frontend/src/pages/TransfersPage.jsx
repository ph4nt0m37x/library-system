import { useEffect, useState } from "react";
import keycloak from "../keycloak";

function TransfersPage() {
  const [transfers, setTransfers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadTransfers = async () => {
      try {
        const response = await fetch(
          "http://localhost:8000/api/transfers",
          {
            headers: {
              Authorization: `Bearer ${keycloak.token}`,
            },
          }
        );

        if (!response.ok) {
          throw new Error(
            `Failed to fetch transfers: ${response.status}`
          );
        }

        const data = await response.json();

        console.log("Transfers:", data);
        setTransfers(data);
      } catch (error) {
        console.error(error);
        setError("Could not load transfers.");
      } finally {
        setLoading(false);
      }
    };

    loadTransfers();
  }, []);

  if (loading) {
    return <p>Loading transfers...</p>;
  }

  if (error) {
    return <p>{error}</p>;
  }

  return (
    <div>
      <h1>Transfers</h1>

      <button>Create Transfer</button>

      {transfers.length === 0 ? (
        <p>No transfers found.</p>
      ) : (
        <div>
          {transfers.map((transfer) => (
            <div
              key={
                transfer.id?.value ??
                transfer.id?.id ??
                transfer.id
              }
            >
              <h2>
                Transfer{" "}
                {transfer.id?.value ??
                  transfer.id?.id ??
                  transfer.id}
              </h2>

              <p>
                Book ID:{" "}
                {transfer.bookId?.value ??
                  transfer.bookId?.id ??
                  transfer.bookId}
              </p>

              <p>
                Source Library:{" "}
                {transfer.sourceLibraryId?.value ??
                  transfer.sourceLibraryId?.id ??
                  transfer.sourceLibraryId}
              </p>

              <p>
                Destination Library:{" "}
                {transfer.destinationLibraryId?.value ??
                  transfer.destinationLibraryId?.id ??
                  transfer.destinationLibraryId}
              </p>

              <p>
                Status: {transfer.status}
              </p>

              <p>
                Requested By: {transfer.requestedBy}
              </p>

              <hr />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default TransfersPage;
