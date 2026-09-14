import { useEffect, useState } from "react";
import keycloak from "../keycloak";

function BorrowingPage() {
  const [loans, setLoans] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadLoans = async () => {
      try {
        const response = await fetch(
          "http://localhost:8000/api/loans/all",
          {
            headers: {
              Authorization: `Bearer ${keycloak.token}`,
            },
          }
        );

        if (!response.ok) {
          throw new Error(`Failed to fetch loans: ${response.status}`);
        }

        const data = await response.json();

        console.log("Loans:", data);
        setLoans(data);
      } catch (error) {
        console.error(error);
        setError("Could not load borrowing information.");
      } finally {
        setLoading(false);
      }
    };

    loadLoans();
  }, []);

  if (loading) return <p>Loading borrowing information...</p>;
  if (error) return <p>{error}</p>;

  return (
    <div>
      <h1>Borrowing</h1>

      <button>Borrow Book</button>
      <button>Return Book</button>

      {loans.length === 0 ? (
        <p>No loans found.</p>
      ) : (
        <div>
          {loans.map((loan) => (
            <div key={loan.id}>
              <h2>Loan {loan.id}</h2>

              <p>Member ID: {loan.memberId}</p>
              <p>Book ID: {loan.bookId}</p>
              <p>Library ID: {loan.libraryId}</p>
              <p>Status: {loan.status}</p>
              <p>Borrowed At: {loan.borrowedAt}</p>
              <p>Returned At: {loan.returnedAt ?? "Not returned"}</p>

              <hr />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default BorrowingPage;
