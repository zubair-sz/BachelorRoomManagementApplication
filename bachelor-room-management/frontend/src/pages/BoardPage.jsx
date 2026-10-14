import MediaBoard from "../components/MediaBoard";
import { useRooms } from "../context/RoomContext";

export default function BoardPage() {
  const { activeRoom: room, isAdmin } = useRooms();
  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="mb-1 text-xl font-semibold text-gray-900">Board</h1>
      <p className="mb-5 text-sm text-gray-500">Updates, photos and videos for {room.name}</p>
      <MediaBoard key={room.id} room={room} isAdmin={isAdmin} />
    </div>
  );
}
