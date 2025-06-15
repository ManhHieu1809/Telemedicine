const ReviewManagement = () => {
    const [reviews, setReviews] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedReview, setSelectedReview] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [filterStatus, setFilterStatus] = useState('all');

    const fetchReviews = async () => {
        try {
            setLoading(true);
            // Mock data - replace with actual API call
            const mockReviews = [
                {
                    id: 1,
                    patientName: "Nguyễn Văn A",
                    doctorName: "BS. Trần Thị B",
                    rating: 5,
                    comment: "Bác sĩ rất tận tình và chuyên nghiệp",
                    createdAt: "2024-01-15T10:30:00",
                    status: "APPROVED"
                },
                {
                    id: 2,
                    patientName: "Lê Thị C",
                    doctorName: "BS. Phạm Văn D",
                    rating: 2,
                    comment: "Thái độ không thân thiện, tư vấn không rõ ràng",
                    createdAt: "2024-01-14T14:20:00",
                    status: "PENDING"
                },
                {
                    id: 3,
                    patientName: "Hoàng Minh E",
                    doctorName: "BS. Nguyễn Thị F",
                    rating: 4,
                    comment: "Khám bệnh kỹ lưỡng, giải thích rõ ràng",
                    createdAt: "2024-01-13T09:15:00",
                    status: "APPROVED"
                },
                {
                    id: 4,
                    patientName: "Trần Văn G",
                    doctorName: "BS. Lê Thị H",
                    rating: 1,
                    comment: "Nội dung không phù hợp, sử dụng từ ngữ xúc phạm",
                    createdAt: "2024-01-12T16:45:00",
                    status: "REJECTED"
                }
            ];
            setReviews(mockReviews);
        } catch (error) {
            console.error('Error fetching reviews:', error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchReviews();
    }, []);

    const handleApproveReview = async (reviewId) => {
        try {
            // await api.put(`/admin/reviews/${reviewId}/approve`);
            setReviews(prev => prev.map(review =>
                review.id === reviewId ? { ...review, status: 'APPROVED' } : review
            ));
            alert('Đánh giá đã được duyệt thành công');
        } catch (error) {
            alert('Lỗi khi duyệt đánh giá');
        }
    };

    const handleRejectReview = async (reviewId) => {
        try {
            // await api.put(`/admin/reviews/${reviewId}/reject`);
            setReviews(prev => prev.map(review =>
                review.id === reviewId ? { ...review, status: 'REJECTED' } : review
            ));
            alert('Đánh giá đã được từ chối');
        } catch (error) {
            alert('Lỗi khi từ chối đánh giá');
        }
    };

    const handleDeleteReview = async (reviewId) => {
        if (!window.confirm('Bạn có chắc chắn muốn xóa đánh giá này?')) return;

        try {
            await api.delete(`/admin/reviews/${reviewId}`);
            setReviews(prev => prev.filter(review => review.id !== reviewId));
            alert('Đánh giá đã được xóa');
        } catch (error) {
            alert('Lỗi khi xóa đánh giá');
        }
    };

    const getStatusColor = (status) => {
        const colors = {
            'PENDING': 'bg-yellow-100 text-yellow-800',
            'APPROVED': 'bg-green-100 text-green-800',
            'REJECTED': 'bg-red-100 text-red-800'
        };
        return colors[status] || 'bg-gray-100 text-gray-800';
    };

    const getStatusText = (status) => {
        const texts = {
            'PENDING': 'Chờ duyệt',
            'APPROVED': 'Đã duyệt',
            'REJECTED': 'Đã từ chối'
        };
        return texts[status] || status;
    };

    const getRatingStars = (rating) => {
        return '⭐'.repeat(rating) + '☆'.repeat(5 - rating);
    };

    const filteredReviews = reviews.filter(review =>
        filterStatus === 'all' || review.status === filterStatus
    );

    const exportReviews = (format) => {
        const data = filteredReviews.map(review => ({
            'ID': review.id,
            'Bệnh nhân': review.patientName,
            'Bác sĩ': review.doctorName,
            'Đánh giá': review.rating,
            'Bình luận': review.comment,
            'Trạng thái': getStatusText(review.status),
            'Ngày tạo': formatDateTime(review.createdAt)
        }));

        if (format === 'pdf') {
            exportUtils.exportToPDF('Báo cáo đánh giá',
                data.map(item => Object.entries(item).map(([k,v]) => `${k}: ${v}`).join(', '))
            );
        } else {
            exportUtils.exportToExcel('Báo cáo đánh giá', data);
        }
    };

    if (loading) return <LoadingSpinner />;

    return (
        <div className="space-y-6 fade-in">
            <div className="flex justify-between items-center">
                <h2 className="text-2xl font-bold">Quản lý đánh giá</h2>
                <div className="flex items-center space-x-4">
                    <select
                        value={filterStatus}
                        onChange={(e) => setFilterStatus(e.target.value)}
                        className="border rounded-lg px-3 py-2"
                    >
                        <option value="all">Tất cả</option>
                        <option value="PENDING">Chờ duyệt</option>
                        <option value="APPROVED">Đã duyệt</option>
                        <option value="REJECTED">Đã từ chối</option>
                    </select>
                    <div className="space-x-2">
                        <button
                            onClick={() => exportReviews('pdf')}
                            className="bg-red-600 text-white px-3 py-1 rounded text-sm hover:bg-red-700"
                        >
                            📄 PDF
                        </button>
                        <button
                            onClick={() => exportReviews('excel')}
                            className="bg-green-600 text-white px-3 py-1 rounded text-sm hover:bg-green-700"
                        >
                            📊 Excel
                        </button>
                    </div>
                </div>
            </div>

            <div className="bg-white rounded-xl shadow-lg overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full">
                        <thead className="bg-gray-50">
                        <tr>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Bệnh nhân</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Bác sĩ</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Đánh giá</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Bình luận</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Trạng thái</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200">
                        {filteredReviews.map((review) => (
                            <tr key={review.id} className="hover:bg-gray-50">
                                <td className="px-6 py-4 font-medium">{review.patientName}</td>
                                <td className="px-6 py-4">{review.doctorName}</td>
                                <td className="px-6 py-4">
                                    <div className="flex items-center">
                                        <span className="text-lg">{getRatingStars(review.rating)}</span>
                                        <span className="ml-2 text-sm text-gray-600">({review.rating}/5)</span>
                                    </div>
                                </td>
                                <td className="px-6 py-4 max-w-xs">
                                    <p className="truncate" title={review.comment}>
                                        {review.comment}
                                    </p>
                                </td>
                                <td className="px-6 py-4">
                                        <span className={`px-2 py-1 text-xs rounded-full ${getStatusColor(review.status)}`}>
                                            {getStatusText(review.status)}
                                        </span>
                                </td>
                                <td className="px-6 py-4 text-sm space-x-2">
                                    {review.status === 'PENDING' && (
                                        <>
                                            <button
                                                onClick={() => handleApproveReview(review.id)}
                                                className="text-green-600 hover:text-green-900"
                                            >
                                                Duyệt
                                            </button>
                                            <button
                                                onClick={() => handleRejectReview(review.id)}
                                                className="text-orange-600 hover:text-orange-900"
                                            >
                                                Từ chối
                                            </button>
                                        </>
                                    )}
                                    <button
                                        onClick={() => {
                                            setSelectedReview(review);
                                            setIsModalOpen(true);
                                        }}
                                        className="text-blue-600 hover:text-blue-900"
                                    >
                                        Xem
                                    </button>
                                    <button
                                        onClick={() => handleDeleteReview(review.id)}
                                        className="text-red-600 hover:text-red-900"
                                    >
                                        Xóa
                                    </button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            </div>

            <Modal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                title="Chi tiết đánh giá"
            >
                {selectedReview && (
                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700">Bệnh nhân</label>
                            <p className="text-gray-900">{selectedReview.patientName}</p>
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700">Bác sĩ</label>
                            <p className="text-gray-900">{selectedReview.doctorName}</p>
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700">Đánh giá</label>
                            <p className="text-2xl">{getRatingStars(selectedReview.rating)}</p>
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700">Bình luận</label>
                            <p className="text-gray-900 bg-gray-50 p-3 rounded-lg">{selectedReview.comment}</p>
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700">Thời gian</label>
                            <p className="text-gray-900">{formatDateTime(selectedReview.createdAt)}</p>
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700">Trạng thái</label>
                            <span className={`px-2 py-1 text-xs rounded-full ${getStatusColor(selectedReview.status)}`}>
                                {getStatusText(selectedReview.status)}
                            </span>
                        </div>

                        {/* Quick actions in modal */}
                        {selectedReview.status === 'PENDING' && (
                            <div className="flex space-x-3 pt-4 border-t">
                                <button
                                    onClick={() => {
                                        handleApproveReview(selectedReview.id);
                                        setIsModalOpen(false);
                                    }}
                                    className="flex-1 bg-green-600 text-white py-2 rounded-lg hover:bg-green-700"
                                >
                                    ✅ Duyệt đánh giá
                                </button>
                                <button
                                    onClick={() => {
                                        handleRejectReview(selectedReview.id);
                                        setIsModalOpen(false);
                                    }}
                                    className="flex-1 bg-orange-600 text-white py-2 rounded-lg hover:bg-orange-700"
                                >
                                    ❌ Từ chối
                                </button>
                            </div>
                        )}
                    </div>
                )}
            </Modal>
        </div>
    );
};