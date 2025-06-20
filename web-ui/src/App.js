import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import axios from 'axios';
import './App.css';

function App() {
  const [requests, setRequests] = useState([]);
  const [deployments, setDeployments] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // In a real app, you would fetch this data from your API
    const fetchData = async () => {
      try {
        // Mock data for demonstration
        setRequests([
          { id: 1, from: 'user@example.com', subject: 'Create a simple web server', status: 'completed', timestamp: '2023-06-20T10:30:00' },
          { id: 2, from: 'dev@example.com', subject: 'Generate data processing script', status: 'processing', timestamp: '2023-06-20T11:15:00' },
        ]);
        
        setDeployments([
          { id: 1, appName: 'web-server', status: 'running', timestamp: '2023-06-20T10:35:00', endpoint: 'http://localhost:3001' },
          { id: 2, appName: 'data-processor', status: 'deploying', timestamp: '2023-06-20T11:20:00' },
        ]);
        
        setLoading(false);
      } catch (error) {
        console.error('Error fetching data:', error);
        setLoading(false);
      }
    };

    fetchData();
    
    // Set up polling
    const interval = setInterval(fetchData, 30000);
    
    return () => clearInterval(interval);
  }, []);

  if (loading) {
    return <div className="loading">Loading...</div>;
  }

  return (
    <Router>
      <div className="app">
        <header className="app-header">
          <h1>EMLLM - Application Distribution System</h1>
          <nav>
            <Link to="/">Dashboard</Link>
            <Link to="/requests">Requests</Link>
            <Link to="/deployments">Deployments</Link>
          </nav>
        </header>
        
        <main className="app-content">
          <Routes>
            <Route path="/" element={
              <Dashboard 
                requests={requests} 
                deployments={deployments} 
              />
            } />
            <Route path="/requests" element={
              <RequestList requests={requests} />
            } />
            <Route path="/deployments" element={
              <DeploymentList deployments={deployments} />
            } />
          </Routes>
        </main>
        
        <footer className="app-footer">
          <p>EMLLM Application Distribution System &copy; {new Date().getFullYear()}</p>
        </footer>
      </div>
    </Router>
  );
}

function Dashboard({ requests, deployments }) {
  const recentRequests = [...requests].slice(0, 5);
  const recentDeployments = [...deployments].slice(0, 5);
  
  return (
    <div>
      <section className="dashboard-stats">
        <div className="stat-card">
          <h3>Total Requests</h3>
          <p className="stat-number">{requests.length}</p>
        </div>
        <div className="stat-card">
          <h3>Active Deployments</h3>
          <p className="stat-number">
            {deployments.filter(d => d.status === 'running').length}
          </p>
        </div>
        <div className="stat-card">
          <h3>Pending Requests</h3>
          <p className="stat-number">
            {requests.filter(r => r.status === 'processing').length}
          </p>
        </div>
      </section>
      
      <section className="dashboard-sections">
        <div className="dashboard-section">
          <h2>Recent Requests</h2>
          <RequestList requests={recentRequests} showViewAll={requests.length > 5} />
        </div>
        
        <div className="dashboard-section">
          <h2>Recent Deployments</h2>
          <DeploymentList deployments={recentDeployments} showViewAll={deployments.length > 5} />
        </div>
      </section>
    </div>
  );
}

function RequestList({ requests, showViewAll = false }) {
  return (
    <div className="card">
      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>From</th>
              <th>Subject</th>
              <th>Status</th>
              <th>Time</th>
            </tr>
          </thead>
          <tbody>
            {requests.length > 0 ? (
              requests.map(request => (
                <tr key={request.id}>
                  <td>{request.from}</td>
                  <td>{request.subject}</td>
                  <td>
                    <span className={`status-badge ${request.status}`}>
                      {request.status}
                    </span>
                  </td>
                  <td>{new Date(request.timestamp).toLocaleString()}</td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="4" className="no-data">No requests found</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      {showViewAll && (
        <div className="card-footer">
          <Link to="/requests" className="view-all">View All Requests</Link>
        </div>
      )}
    </div>
  );
}

function DeploymentList({ deployments, showViewAll = false }) {
  return (
    <div className="card">
      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Application</th>
              <th>Status</th>
              <th>Endpoint</th>
              <th>Deployed At</th>
            </tr>
          </thead>
          <tbody>
            {deployments.length > 0 ? (
              deployments.map(deployment => (
                <tr key={deployment.id}>
                  <td>{deployment.appName}</td>
                  <td>
                    <span className={`status-badge ${deployment.status}`}>
                      {deployment.status}
                    </span>
                  </td>
                  <td>
                    {deployment.endpoint ? (
                      <a href={deployment.endpoint} target="_blank" rel="noopener noreferrer">
                        {deployment.endpoint}
                      </a>
                    ) : 'N/A'}
                  </td>
                  <td>{new Date(deployment.timestamp).toLocaleString()}</td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="4" className="no-data">No deployments found</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      {showViewAll && (
        <div className="card-footer">
          <Link to="/deployments" className="view-all">View All Deployments</Link>
        </div>
      )}
    </div>
  );
}

export default App;
